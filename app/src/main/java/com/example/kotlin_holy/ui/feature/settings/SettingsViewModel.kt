package com.example.kotlin_holy.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.audio.QuranPlayer
import com.example.kotlin_holy.data.audio.ReciterCatalog
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.domain.model.DataMeta
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.Reciter
import com.example.kotlin_holy.domain.repository.AppSettings
import com.example.kotlin_holy.domain.repository.AudioRepository
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Qurilmada saqlangan bitta suraning qiroati */
data class StoredRecitation(
    val surahNumber: Int,
    val nameUz: String,
    val bytes: Long,
)

data class AudioStorage(
    /** Barcha qorilar bo'yicha egallangan joy */
    val totalBytes: Long = 0,
    /** Tanlangan qori uchun yuklangan suralar */
    val recitations: List<StoredRecitation> = emptyList(),
)

data class SettingsState(
    val settings: AppSettings = AppSettings(),
    val meta: DataMeta? = null,
    val readPages: Map<Int, Long> = emptyMap(),
    val audio: AudioStorage = AudioStorage(),
) {
    val readCount: Int get() = readPages.size
    val percent: Float get() = readCount.toFloat() / JuzCatalog.TOTAL_PAGES

    val reciter: Reciter get() = ReciterCatalog.byId(settings.reciterId)

    /** Oxirgi belgilangan bet: vaqtlar teng bo'lsa, eng katta raqamli bet */
    val lastMarked: Pair<Int, Long>?
        get() = readPages.entries
            .maxWithOrNull(compareBy({ it.value }, { it.key }))
            ?.let { it.key to it.value }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val xatmRepository: XatmRepository,
    private val audioRepository: AudioRepository,
    private val player: QuranPlayer,
    quranRepository: QuranRepository,
) : ViewModel() {

    private val meta = MutableStateFlow<DataMeta?>(null)
    private val surahNames = MutableStateFlow<Map<Int, String>>(emptyMap())

    /** Egallangan joy faylma-fayl sanaladi, shuning uchun alohida saqlanadi */
    private val totalBytes = MutableStateFlow(0L)

    val state: StateFlow<SettingsState> = combine(
        settingsRepository.settings,
        meta,
        xatmRepository.readPages,
        combine(audioRepository.downloads, totalBytes) { downloads, total -> downloads to total },
        surahNames,
    ) { settings, dataMeta, pages, (downloads, total), names ->
        SettingsState(
            settings = settings,
            meta = dataMeta,
            readPages = pages,
            audio = AudioStorage(
                totalBytes = total,
                recitations = downloads
                    .mapNotNull { (surah, state) ->
                        (state as? AudioDownload.Ready)?.let {
                            StoredRecitation(
                                surahNumber = surah,
                                nameUz = names[surah] ?: "$surah-sura",
                                bytes = it.bytes,
                            )
                        }
                    }
                    .sortedBy { it.surahNumber },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())

    init {
        viewModelScope.launch { meta.value = quranRepository.meta() }
        viewModelScope.launch {
            surahNames.value = runCatching {
                quranRepository.surahs().associate { it.number to it.nameUz }
            }.getOrDefault(emptyMap())
        }
        /* Yuklangan yoki o'chirilgan sura bo'lsa, egallangan joy qayta sanaladi */
        viewModelScope.launch {
            audioRepository.downloads.collect { refreshStorage() }
        }
    }

    private suspend fun refreshStorage() {
        totalBytes.value = runCatching { audioRepository.storageBytes() }.getOrDefault(0L)
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }

    fun setArabicScale(value: Float) =
        viewModelScope.launch { settingsRepository.setArabicScale(value) }

    fun setTranslationScale(value: Float) =
        viewModelScope.launch { settingsRepository.setTranslationScale(value) }

    /**
     * Qori almashtirilganda ijro to'xtatiladi: eski qori fayli ochiq turgan
     * bo'lsa, yangi tanlov bilan aralashib ketmasligi kerak. Eski yuklamalar
     * o'chirilmaydi — ular o'z papkasida qoladi va qori qaytarilsa yana ishlaydi.
     */
    fun setReciter(id: Int) = viewModelScope.launch {
        player.stop()
        settingsRepository.setReciter(id)
        refreshStorage()
    }

    fun deleteRecitation(surahNumber: Int) = viewModelScope.launch {
        audioRepository.delete(surahNumber)
        refreshStorage()
    }

    fun deleteAllRecitations() = viewModelScope.launch {
        player.stop()
        audioRepository.deleteAll()
        refreshStorage()
    }

    fun markRange(from: Int, to: Int, read: Boolean) = viewModelScope.launch {
        xatmRepository.setRange(from, to, read)
    }

    fun resetXatm() = viewModelScope.launch { xatmRepository.reset() }
}
