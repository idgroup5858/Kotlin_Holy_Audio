package com.example.kotlin_holy.ui.feature.pageread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class PageReadState(
    val number: Int = 1,
    val loading: Boolean = true,
    val content: PageContent? = null,
    val mushaf: MushafPage? = null,
    val readAt: Long? = null,
    val textMode: String = "both",
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    val highlightedVerse: String? = null,
) {
    val showArabic: Boolean get() = textMode != "translation"
    val showTranslation: Boolean get() = textMode != "arabic"
}

@HiltViewModel
class PageReadViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pageNumber: Int = savedStateHandle.get<String>("number")?.toIntOrNull() ?: 1

    private val content = MutableStateFlow<PageContent?>(null)
    private val mushaf = MutableStateFlow<MushafPage?>(null)
    private val loading = MutableStateFlow(true)
    private val highlighted = MutableStateFlow<String?>(null)

    val state: StateFlow<PageReadState> = combine(
        content,
        mushaf,
        loading,
        highlighted,
        combine(xatmRepository.readPages, settingsRepository.settings) { pages, settings ->
            pages[pageNumber] to settings
        },
    ) { pageContent, mushafPage, isLoading, highlightedVerse, (readAt, settings) ->
        PageReadState(
            number = pageNumber,
            loading = isLoading,
            content = pageContent,
            mushaf = mushafPage,
            readAt = readAt,
            textMode = settings.textMode,
            arabicScale = settings.arabicScale,
            translationScale = settings.translationScale,
            highlightedVerse = highlightedVerse,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PageReadState(number = pageNumber),
    )

    init {
        viewModelScope.launch {
            content.value = runCatching { quranRepository.page(pageNumber) }.getOrNull()
            mushaf.value = runCatching { quranRepository.mushafPage(pageNumber) }.getOrNull()
            loading.value = false
            settingsRepository.setLastRead("page/$pageNumber", "$pageNumber-sahifa")
        }
    }

    fun toggleRead() {
        viewModelScope.launch { xatmRepository.toggle(pageNumber) }
    }

    fun onVerseClick(verseKey: String) {
        highlighted.value = if (highlighted.value == verseKey) null else verseKey
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }
}
