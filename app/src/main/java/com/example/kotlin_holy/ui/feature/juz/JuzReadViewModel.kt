package com.example.kotlin_holy.ui.feature.juz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.JuzInfo
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.PageContent
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

data class JuzReadState(
    val juz: JuzInfo? = null,
    val loading: Boolean = true,
    val pageNumber: Int = 1,
    val mushaf: MushafPage? = null,
    val content: PageContent? = null,
    val readAt: Long? = null,
    val textMode: String = "both",
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    val highlightedVerse: String? = null,
) {
    val showArabic: Boolean get() = textMode != "translation"
    val showTranslation: Boolean get() = textMode != "arabic"
}

/** Juz bet-bet o'qiladi: juzning birinchi betidan oxirgi betigacha */
@HiltViewModel
class JuzReadViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val juzNumber: Int = savedStateHandle.get<String>("number")?.toIntOrNull() ?: 1
    private val juz = JuzCatalog.byNumber(juzNumber)

    private val pageNumber = MutableStateFlow(juz?.fromPage ?: 1)
    private val mushaf = MutableStateFlow<MushafPage?>(null)
    private val content = MutableStateFlow<PageContent?>(null)
    private val loading = MutableStateFlow(true)
    private val highlighted = MutableStateFlow<String?>(null)

    val state: StateFlow<JuzReadState> = combine(
        pageNumber,
        mushaf,
        content,
        loading,
        combine(settingsRepository.settings, xatmRepository.readPages, highlighted) { s, pages, hl ->
            Triple(s, pages, hl)
        },
    ) { page, mushafPage, pageContent, isLoading, (settings, pages, highlightedVerse) ->
        JuzReadState(
            juz = juz,
            loading = isLoading,
            pageNumber = page,
            mushaf = mushafPage,
            content = pageContent,
            readAt = pages[page],
            textMode = settings.textMode,
            arabicScale = settings.arabicScale,
            translationScale = settings.translationScale,
            highlightedVerse = highlightedVerse,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JuzReadState(juz = juz))

    init {
        load(pageNumber.value)
        viewModelScope.launch {
            settingsRepository.setLastRead("juz/$juzNumber", "$juzNumber-juz")
        }
    }

    private fun load(page: Int) {
        viewModelScope.launch {
            loading.value = true
            mushaf.value = runCatching { quranRepository.mushafPage(page) }.getOrNull()
            content.value = runCatching { quranRepository.page(page) }.getOrNull()
            loading.value = false
        }
    }

    fun move(delta: Int) {
        val info = juz ?: return
        val next = (pageNumber.value + delta).coerceIn(info.fromPage, info.toPage)
        if (next != pageNumber.value) {
            pageNumber.value = next
            load(next)
        }
    }

    fun toggleRead() {
        viewModelScope.launch { xatmRepository.toggle(pageNumber.value) }
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }

    fun onVerseClick(verseKey: String) {
        highlighted.value = if (highlighted.value == verseKey) null else verseKey
    }
}
