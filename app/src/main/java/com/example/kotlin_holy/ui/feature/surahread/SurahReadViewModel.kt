package com.example.kotlin_holy.ui.feature.surahread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.PageContent
import com.example.kotlin_holy.domain.model.SurahDetail
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sura ichidagi bir bet: mushaf gliflari va shu betning to'liq tarjimasi */
data class SurahPageGroup(
    val pageNumber: Int,
    val juz: Int?,
    val mushaf: MushafPage?,
    val pageContent: PageContent?,
)

data class SurahReadState(
    val loading: Boolean = true,
    val detail: SurahDetail? = null,
    val viewMode: String = "ayah",
    val textMode: String = "both",
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    val cursor: Int = 0,
    val group: SurahPageGroup? = null,
    val readPages: Map<Int, Long> = emptyMap(),
    val highlightedVerse: String? = null,
) {
    val showArabic: Boolean get() = textMode != "translation"
    val showTranslation: Boolean get() = textMode != "arabic"

    /** Suraning betlari — oyatlarning bet raqamidan olinadi */
    val pageNumbers: List<Int>
        get() = detail?.ayahs?.mapNotNull { it.pageNumber }?.distinct().orEmpty()

    val ayahChunks: List<List<Ayah>>
        get() = detail?.ayahs?.chunked(AYAHS_PER_VIEW).orEmpty()

    val xatmRead: Int
        get() {
            val range = detail?.let { JuzCatalog.surahPageRange(it.surah.number) } ?: return 0
            return range.count { readPages.containsKey(it) }
        }

    val xatmTotal: Int
        get() = detail?.let { JuzCatalog.surahPageRange(it.surah.number)?.count() } ?: 0

    companion object {
        const val AYAHS_PER_VIEW = 10
    }
}

@HiltViewModel
class SurahReadViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<String>("number")?.toIntOrNull() ?: 1

    private val detail = MutableStateFlow<SurahDetail?>(null)
    private val loading = MutableStateFlow(true)
    private val cursor = MutableStateFlow(0)
    private val group = MutableStateFlow<SurahPageGroup?>(null)
    private val highlighted = MutableStateFlow<String?>(null)

    val state: StateFlow<SurahReadState> = combine(
        detail,
        loading,
        cursor,
        group,
        combine(settingsRepository.settings, xatmRepository.readPages, highlighted) { s, pages, hl ->
            Triple(s, pages, hl)
        },
    ) { surahDetail, isLoading, position, pageGroup, (settings, pages, highlightedVerse) ->
        SurahReadState(
            loading = isLoading,
            detail = surahDetail,
            viewMode = settings.viewMode,
            textMode = settings.textMode,
            arabicScale = settings.arabicScale,
            translationScale = settings.translationScale,
            cursor = position,
            group = pageGroup,
            readPages = pages,
            highlightedVerse = highlightedVerse,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurahReadState())

    init {
        viewModelScope.launch {
            detail.value = runCatching { quranRepository.surah(surahNumber) }.getOrNull()
            loading.value = false
            detail.value?.ayahs?.firstOrNull()?.let { saveLastReadAyah(it) }
            loadGroup(0, trackLastRead = false)
        }
    }

    /**
     * "Davom etish" har doim jismoniy bet raqamiga ("page/{number}") ishora
     * qiladi — chunki faqat sahifa marshruti har safar aniq o'sha betdan
     * qayta ochilishini kafolatlaydi. Oyat bo'yicha o'qishda ham shu bet
     * ochiladi, faqat yorliq oyat raqamini ko'rsatadi (masalan "Rum 30:6").
     */
    private fun saveLastReadAyah(ayah: Ayah) {
        val pageNumber = ayah.pageNumber ?: return
        val surahName = detail.value?.surah?.nameUz ?: return
        viewModelScope.launch {
            settingsRepository.setLastRead(
                "page/$pageNumber",
                "$surahName $surahNumber:${ayah.numberInSurah}",
            )
        }
    }

    private fun saveLastReadPage(pageNumber: Int) {
        viewModelScope.launch {
            settingsRepository.setLastRead("page/$pageNumber", "$pageNumber-sahifa")
        }
    }

    /** Sahifa bo'yicha ko'rinish uchun kerakli betni yuklaymiz */
    private fun loadGroup(position: Int, trackLastRead: Boolean = true) {
        val pages = state.value.pageNumbers.ifEmpty {
            detail.value?.ayahs?.mapNotNull { it.pageNumber }?.distinct().orEmpty()
        }
        val pageNumber = pages.getOrNull(position) ?: return
        if (trackLastRead) saveLastReadPage(pageNumber)
        viewModelScope.launch {
            val mushaf = runCatching { quranRepository.mushafPage(pageNumber) }.getOrNull()
            val content = runCatching { quranRepository.page(pageNumber) }.getOrNull()
            group.value = SurahPageGroup(
                pageNumber = pageNumber,
                juz = content?.juz,
                mushaf = mushaf,
                pageContent = content,
            )
        }
    }

    fun move(delta: Int) {
        val next = (cursor.value + delta).coerceAtLeast(0)
        cursor.value = next
        if (state.value.viewMode == "page") {
            loadGroup(next)
        } else {
            state.value.ayahChunks.getOrNull(next)?.firstOrNull()?.let { saveLastReadAyah(it) }
        }
    }

    fun setViewMode(mode: String) {
        viewModelScope.launch {
            settingsRepository.setViewMode(mode)
            cursor.value = 0
            if (mode == "page") {
                loadGroup(0)
            } else {
                detail.value?.ayahs?.firstOrNull()?.let { saveLastReadAyah(it) }
            }
        }
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }

    fun toggleCurrentPage() {
        val page = group.value?.pageNumber ?: return
        viewModelScope.launch { xatmRepository.toggle(page) }
    }

    fun onVerseClick(verseKey: String) {
        highlighted.value = if (highlighted.value == verseKey) null else verseKey
    }
}
