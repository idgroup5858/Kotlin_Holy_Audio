package com.example.kotlin_holy.ui.feature.pages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Surah
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

/** Betda qaysi sura boshlanadi yoki davom etadi */
data class PageSurahInfo(val starting: List<Surah>, val current: Surah?)

data class PageListState(
    val loading: Boolean = true,
    val pages: List<PageSummary> = emptyList(),
    val surahByPage: Map<Int, PageSurahInfo> = emptyMap(),
    val readPages: Map<Int, Long> = emptyMap(),
    val order: String = "asc",
    val onlyUnread: Boolean = false,
) {
    /** Tartib va «faqat oʻqilmaganlar» filtri */
    val visible: List<PageSummary>
        get() {
            val filtered = if (onlyUnread) pages.filter { !readPages.containsKey(it.number) } else pages
            return if (order == "desc") filtered.sortedByDescending { it.number } else filtered
        }
}

@HiltViewModel
class PageListViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val pages = MutableStateFlow<List<PageSummary>>(emptyList())
    private val surahInfo = MutableStateFlow<Map<Int, PageSurahInfo>>(emptyMap())
    private val loading = MutableStateFlow(true)
    private val onlyUnread = MutableStateFlow(false)

    val state: StateFlow<PageListState> = combine(
        pages,
        surahInfo,
        loading,
        combine(xatmRepository.readPages, onlyUnread) { read, unread -> read to unread },
        settingsRepository.settings,
    ) { list, info, isLoading, (read, unread), settings ->
        PageListState(
            loading = isLoading,
            pages = list,
            surahByPage = info,
            readPages = read,
            order = settings.pageOrder,
            onlyUnread = unread,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PageListState())

    init {
        viewModelScope.launch {
            pages.value = runCatching { quranRepository.pages() }.getOrDefault(emptyList())
            val surahs = runCatching { quranRepository.surahs() }.getOrDefault(emptyList())
            surahInfo.value = buildSurahByPage(surahs)
            loading.value = false
        }
    }

    fun toggleOrder() {
        val next = if (state.value.order == "asc") "desc" else "asc"
        viewModelScope.launch { settingsRepository.setPageOrder(next) }
    }

    fun toggleOnlyUnread() {
        onlyUnread.value = !onlyUnread.value
    }

    fun toggle(page: Int) {
        viewModelScope.launch { xatmRepository.toggle(page) }
    }
}

/** Har bir bet uchun: shu betda boshlanadigan suralar va davom etayotgan sura */
private fun buildSurahByPage(surahs: List<Surah>): Map<Int, PageSurahInfo> {
    val sorted = surahs.sortedBy { it.number }
    val result = HashMap<Int, PageSurahInfo>(604)
    var cursor = 0
    var current: Surah? = null
    for (page in 1..604) {
        val starting = mutableListOf<Surah>()
        while (cursor < sorted.size && sorted[cursor].startPage <= page) {
            if (sorted[cursor].startPage == page) starting += sorted[cursor]
            current = sorted[cursor]
            cursor++
        }
        result[page] = PageSurahInfo(starting, current)
    }
    return result
}
