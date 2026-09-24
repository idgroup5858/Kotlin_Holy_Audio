package com.example.kotlin_holy.data.audio

import com.example.kotlin_holy.data.local.AssetJsonSource
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.domain.model.AyahTiming
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.SurahAudio
import com.example.kotlin_holy.domain.model.WordSegment
import com.example.kotlin_holy.domain.repository.AudioRepository
import com.example.kotlin_holy.domain.repository.DEFAULT_RECITER_ID
import com.example.kotlin_holy.domain.repository.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sura audiosini yuklaydi, saqlaydi va ijro uchun beradi.
 *
 * Yuklash tartibi ataylab shunday: avval vaqt jadvali yoziladi, keyin mp3.
 * Mp3 esa ".part" nomi bilan yuklanib, faqat to'liq tugagach asl nomiga
 * o'tkaziladi — shu sababli yarim yuklangan sura hech qachon "tayyor" deb
 * ko'rsatilmaydi va ijroda ochilib qolmaydi.
 */
@Singleton
class AudioRepositoryImpl @Inject constructor(
    private val remote: AudioRemoteSource,
    private val store: AudioFileStore,
    private val assets: AssetJsonSource,
    settingsRepository: SettingsRepository,
) : AudioRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val states = MutableStateFlow<Map<Int, AudioDownload>>(emptyMap())
    private val jobs = ConcurrentHashMap<Int, Job>()

    private val reciter = MutableStateFlow(DEFAULT_RECITER_ID)
    private val reciterId: Int get() = reciter.value

    override val downloads: StateFlow<Map<Int, AudioDownload>> = states

    init {
        scope.launch {
            settingsRepository.settings
                .map { it.reciterId }
                .distinctUntilChanged()
                .collect { id ->
                    reciter.value = id
                    /* Qori almashsa, holatlar boshqa papkaga tegishli bo'ladi */
                    states.value = emptyMap()
                    refreshReadyStates()
                }
        }
    }

    override fun downloadState(surahNumber: Int): Flow<AudioDownload> =
        states.map { it[surahNumber] ?: AudioDownload.Absent() }.distinctUntilChanged()

    override suspend fun audio(surahNumber: Int): SurahAudio? = withContext(Dispatchers.IO) {
        val id = reciterId
        if (!store.isReady(id, surahNumber)) return@withContext null
        runCatching {
            val stored = json.decodeFromString<StoredTimingsDto>(
                store.timingsFile(id, surahNumber).readText(),
            )
            /* Avval yuklangan jadvallarda eski, qattiqroq tekshiruv natijasi
               saqlangan — shuning uchun ishonchlilik har safar qayta baholanadi */
            val glyphCounts = glyphWordCounts(stored.surahNumber)
            SurahAudio(
                surahNumber = stored.surahNumber,
                reciterId = stored.reciterId,
                timings = stored.ayahs.map {
                    it.toDomain(stored.surahNumber, glyphCounts[it.ayah])
                },
            )
        }.getOrNull()
    }

    override fun audioFile(surahNumber: Int): File? =
        store.audioFile(reciterId, surahNumber).takeIf { store.isReady(reciterId, surahNumber) }

    override suspend fun expectedSize(surahNumber: Int): Long? {
        val info = remote.chapterAudio(reciterId, surahNumber) ?: return null
        return remote.sizeOf(info.audioUrl) ?: info.fileSize.toLong().takeIf { it > 0 }
    }

    override suspend fun download(surahNumber: Int) {
        if (jobs[surahNumber]?.isActive == true) return
        val id = reciterId

        jobs[surahNumber] = scope.launch {
            try {
                setState(surahNumber, AudioDownload.InProgress(0, null))

                val info = remote.chapterAudio(id, surahNumber)
                    ?: error("Qiroat ma'lumoti olinmadi")

                /* Vaqt jadvalidagi so'zlar mushafdagi so'zlarga mos kelishini
                   shu yerda tekshiramiz — ijro paytida tekshirilmaydi */
                val glyphCounts = glyphWordCounts(surahNumber)
                val stored = buildStoredTimings(surahNumber, id, info, glyphCounts)

                store.ensureDir(id)
                store.timingsFile(id, surahNumber).writeText(json.encodeToString(stored))

                val declared = remote.sizeOf(info.audioUrl)
                    ?: info.fileSize.toLong().takeIf { it > 0 }

                val part = store.partFile(id, surahNumber)
                remote.download(info.audioUrl, part) { done, total ->
                    setState(surahNumber, AudioDownload.InProgress(done, total ?: declared))
                }.getOrThrow()

                val target = store.audioFile(id, surahNumber)
                target.delete()
                if (!part.renameTo(target)) error("Fayl saqlanmadi")

                setState(surahNumber, AudioDownload.Ready(store.sizeOf(id, surahNumber)))
            } catch (cancelled: CancellationException) {
                setState(surahNumber, AudioDownload.Absent())
                throw cancelled
            } catch (error: Exception) {
                setState(surahNumber, AudioDownload.Failed(error.message))
            } finally {
                jobs.remove(surahNumber)
            }
        }
    }

    override fun cancel(surahNumber: Int) {
        jobs.remove(surahNumber)?.cancel()
    }

    override suspend fun delete(surahNumber: Int) {
        cancel(surahNumber)
        store.delete(reciterId, surahNumber)
        setState(surahNumber, AudioDownload.Absent())
    }

    override suspend fun deleteAll() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        store.deleteAll()
        states.value = emptyMap()
    }

    override suspend fun storageBytes(): Long = store.totalBytes()

    override suspend fun downloadedSurahs(): List<Int> = store.downloadedSurahs(reciterId)

    /* ---------- ichki yordamchilar ---------- */

    private fun setState(surahNumber: Int, state: AudioDownload) {
        states.value = states.value + (surahNumber to state)
    }

    private suspend fun refreshReadyStates() {
        val id = reciterId
        val ready = store.downloadedSurahs(id).associateWith { surah ->
            AudioDownload.Ready(store.sizeOf(id, surah)) as AudioDownload
        }
        states.value = ready
    }

    /** Suraning har bir oyatidagi so'zlar soni — mushaf gliflaridan sanaladi */
    private suspend fun glyphWordCounts(surahNumber: Int): Map<Int, Int> {
        val range = JuzCatalog.surahPageRange(surahNumber) ?: return emptyMap()
        val counts = HashMap<Int, Int>()
        for (page in range) {
            val mushaf = runCatching { assets.mushafPage(page) }.getOrNull() ?: continue
            for (line in mushaf.lines) {
                for (word in line.words) {
                    if (word.charType == "end" || word.surahNumber != surahNumber) continue
                    counts[word.ayahNumber] = (counts[word.ayahNumber] ?: 0) + 1
                }
            }
        }
        return counts
    }

    /**
     * Segmentlarni saqlanadigan ko'rinishga o'tkazadi.
     *
     * Ba'zi segmentlarda tugash vaqti yo'q (massiv ikki elementli) — bunda
     * keyingi so'zning boshlanishi tugash vaqti deb olinadi.
     */
    private fun buildStoredTimings(
        surahNumber: Int,
        reciterId: Int,
        info: ChapterAudioFileDto,
        glyphCounts: Map<Int, Int>,
    ): StoredTimingsDto {
        val ayahs = info.timestamps.mapNotNull { stamp ->
            val ayahNumber = stamp.verseKey.substringAfter(':').toIntOrNull()
                ?: return@mapNotNull null

            val valid = stamp.segments.filter { it.size >= 2 }
            val words = ArrayList<Long>(valid.size * 3)
            valid.forEachIndexed { index, segment ->
                val start = segment[1]
                val end = segment.getOrNull(2)
                    ?: valid.getOrNull(index + 1)?.get(1)
                    ?: stamp.to
                words += segment[0]
                words += start
                words += end
            }

            val reliable = wordsMatchMushaf(valid.map { it[0] }, glyphCounts[ayahNumber])

            StoredAyahDto(
                ayah = ayahNumber,
                from = stamp.from,
                to = stamp.to,
                reliable = reliable,
                words = words,
            )
        }
        return StoredTimingsDto(surahNumber, reciterId, info.audioUrl, ayahs)
    }
}

/**
 * Audio jadvali so'zlarni mushaf bilan bir xil raqamlaganmi. Ba'zi oyatlarda
 * jadvalda bitta so'zning vaqti tushib qolgan (masalan Baqara 212 da 18-so'z) —
 * raqamlash baribir mos, faqat o'sha so'z yonmaydi. Raqam mushafdagi so'zlar
 * sonidan oshsa esa (Kahf 60) raqamlash boshqacha: bunda noto'g'ri so'z
 * yonmasligi uchun faqat oyat darajasida yonadi.
 */
private fun wordsMatchMushaf(positions: List<Long>, expected: Int?): Boolean =
    expected != null && positions.isNotEmpty() && positions.all { it in 1..expected }

private fun StoredAyahDto.toDomain(surahNumber: Int, expectedWords: Int?): AyahTiming {
    val segments = ArrayList<WordSegment>(words.size / 3)
    var index = 0
    while (index + 2 < words.size) {
        segments += WordSegment(
            position = words[index].toInt(),
            startMs = words[index + 1],
            endMs = words[index + 2],
        )
        index += 3
    }
    return AyahTiming(
        surahNumber = surahNumber,
        ayahNumber = ayah,
        startMs = from,
        endMs = to,
        words = segments,
        wordsReliable = if (expectedWords != null) {
            wordsMatchMushaf(segments.map { it.position.toLong() }, expectedWords)
        } else {
            reliable
        },
    )
}
