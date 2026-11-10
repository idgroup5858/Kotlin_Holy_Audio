package com.example.kotlin_holy.data.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Qiroat internetdan olinadi: avval oyat va so'z vaqtlari jadvali, so'ng
 * suraning yaxlit mp3 fayli.
 *
 * Har ikkalasi uchun zaxira manzil bor. Ba'zi tarmoqlarda (provayder yoki
 * Wi-Fi filtri) api.quran.com va download.quranicaudio.com ochilmaydi, lekin
 * xuddi o'sha ma'lumot api.qurancdn.com va BunnyCDN manzilida turadi —
 * birinchisi ishlamasa, keyingisi sinab ko'riladi va ishlagani eslab qolinadi.
 *
 * Yuklash uzilib qolsa, qaytadan boshlanmaydi — server Range so'rovlarini
 * qo'llab-quvvatlaydi, shuning uchun yarim yuklangan ".part" faylning davomi
 * so'raladi.
 */
@Singleton
class AudioRemoteSource @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Oxirgi marta ishlagan manzillar — keyingi so'rov shundan boshlanadi */
    @Volatile private var apiIndex = 0
    @Volatile private var audioIndex = 0

    /**
     * Sura audiosi haqidagi ma'lumot: havola va vaqt jadvali.
     * Hech bir manzil javob bermasa, sababi bilan xato tashlaydi.
     */
    suspend fun chapterAudio(reciterId: Int, surahNumber: Int): ChapterAudioFileDto =
        withContext(Dispatchers.IO) {
            var lastError: Throwable? = null
            for (offset in API_BASES.indices) {
                val index = (apiIndex + offset) % API_BASES.size
                val url = "${API_BASES[index]}/chapter_recitations/$reciterId/$surahNumber?segments=true"
                val result = runCatching {
                    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Server javobi: ${response.code}")
                        val body = response.body.string()
                        json.decodeFromString<ChapterAudioResponseDto>(body).audioFile
                            ?.takeIf { it.audioUrl.isNotBlank() && it.timestamps.isNotEmpty() }
                            ?: error("Bu qorida $surahNumber-sura qiroati topilmadi")
                    }
                }
                result.onSuccess {
                    apiIndex = index
                    return@withContext it
                }
                lastError = result.exceptionOrNull()
                /* Ma'lumot keldi-yu, lekin sura yo'q — boshqa manzil ham shuni aytadi */
                if (lastError is IllegalStateException) break
            }
            throw describe(lastError)
        }

    /**
     * Faylning haqiqiy hajmi. API ba'zi qorilar uchun 0 qaytargani sababli
     * hajm sarlavhadan so'raladi.
     */
    suspend fun sizeOf(audioUrl: String): Long? = withContext(Dispatchers.IO) {
        for (offset in AUDIO_HOSTS.indices) {
            val index = (audioIndex + offset) % AUDIO_HOSTS.size
            val url = audioUrlFor(audioUrl, index)
            val size = runCatching {
                client.newCall(Request.Builder().url(url).head().build()).execute().use { response ->
                    if (!response.isSuccessful) null
                    else response.header("Content-Length")?.toLongOrNull()
                }
            }.getOrNull()
            if (size != null && size > 0) {
                audioIndex = index
                return@withContext size
            }
        }
        null
    }

    /**
     * Mp3 ni [target] ga yuklaydi. Fayl allaqachon qisman yuklangan bo'lsa,
     * qolgan qismi so'raladi. [onProgress] har bo'lakda chaqiriladi.
     * Asosiy manzil ochilmasa, zaxira manzildan yuklanadi.
     */
    suspend fun download(
        audioUrl: String,
        target: File,
        onProgress: suspend (downloaded: Long, total: Long?) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        var lastError: Throwable? = null
        for (offset in AUDIO_HOSTS.indices) {
            val index = (audioIndex + offset) % AUDIO_HOSTS.size
            val result = runCatching { downloadFrom(audioUrlFor(audioUrl, index), target, onProgress) }
            if (result.isSuccess) {
                audioIndex = index
                return@withContext result
            }
            lastError = result.exceptionOrNull()
            coroutineContext.ensureActive()
        }
        Result.failure(describe(lastError))
    }

    private suspend fun downloadFrom(
        url: String,
        target: File,
        onProgress: suspend (downloaded: Long, total: Long?) -> Unit,
    ) {
        val already = if (target.isFile) target.length() else 0L
        val builder = Request.Builder().url(url)
        if (already > 0) builder.header("Range", "bytes=$already-")

        client.newCall(builder.build()).execute().use { response ->
            /* 416 — fayl allaqachon to'liq yuklangan */
            if (response.code == 416 && already > 0) return
            if (!response.isSuccessful) throw IOException("Server javobi: ${response.code}")
            /* 206 — davomi berildi, 200 — server boshidan qaytardi */
            val resuming = response.code == 206
            val body = response.body

            val startAt = if (resuming) already else 0L
            val total = body.contentLength().takeIf { it > 0 }?.let { it + startAt }

            target.parentFile?.mkdirs()
            var written = startAt
            body.byteStream().use { input ->
                java.io.FileOutputStream(target, resuming).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        written += read
                        onProgress(written, total)
                    }
                    output.flush()
                }
            }
            if (total != null && written < total) throw IOException("Yuklash uzilib qoldi")
        }
    }

    /** Asl havoladagi xostni zaxira xost bilan almashtiradi */
    private fun audioUrlFor(audioUrl: String, index: Int): String {
        val primary = AUDIO_HOSTS.first()
        return if (index == 0 || !audioUrl.startsWith(primary)) audioUrl
        else AUDIO_HOSTS[index] + audioUrl.removePrefix(primary)
    }

    /** Foydalanuvchiga tushunarli xato matni */
    private fun describe(error: Throwable?): Exception = when (error) {
        is IllegalStateException -> error
        is java.net.UnknownHostException -> IOException("Internet aloqasi yoʻq yoki qiroat serveri ochilmadi")
        is java.net.SocketTimeoutException -> IOException("Server javob bermadi — keyinroq urinib koʻring")
        is IOException -> IOException(error.message ?: "Tarmoq xatosi")
        null -> IOException("Qiroat maʼlumoti olinmadi")
        else -> IOException(error.message ?: "Qiroat maʼlumoti olinmadi")
    }

    private companion object {
        /** Bir xil API — quran.com va uning CDN manzili */
        val API_BASES = listOf(
            "https://api.quran.com/api/v4",
            "https://api.qurancdn.com/api/v4",
        )

        /** Bir xil fayllar — asosiy manzil va u turgan BunnyCDN zonasi */
        val AUDIO_HOSTS = listOf(
            "https://download.quranicaudio.com/",
            "https://download-quranicaudio-com.b-cdn.net/",
        )

        const val DOWNLOAD_BUFFER = 64 * 1024
    }
}
