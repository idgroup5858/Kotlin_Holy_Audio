package com.example.kotlin_holy.ui.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.repository.AppSettings
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

data class QuranListsState(
    val loading: Boolean = true,
    val error: String? = null,
    val surahs: List<Surah> = emptyList(),
    val pages: List<PageSummary> = emptyList(),
    val xatm: Map<Int, Long> = emptyMap(),
    val settings: AppSettings = AppSettings(),
)

/**
 * Suralar, Sahifalar, Juzlar va sura / juz betlari ro'yxatlari uchun umumiy holat:
 * ro'yxatlar, xatm belgilari va ko'rinish sozlamalari (katak / ro'yxat, tartib).
 */
@HiltViewModel
class QuranListsViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Sura / juz betlari ekranida yo'ldagi raqam */
    val routeNumber: Int = savedStateHandle.get<String>("number")?.toIntOrNull() ?: 1

    private val data = MutableStateFlow(Triple<List<Surah>, List<PageSummary>, String?>(emptyList(), emptyList(), null))
    private val loading = MutableStateFlow(true)

    val state: StateFlow<QuranListsState> = combine(
        data,
        loading,
        xatmRepository.readPages,
        settingsRepository.settings,
    ) { (surahs, pages, error), isLoading, xatm, settings ->
        QuranListsState(isLoading, error, surahs, pages, xatm, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuranListsState())

    init {
        viewModelScope.launch {
            val surahs = runCatching { quranRepository.surahs() }
            val pages = runCatching { quranRepository.pages() }
            data.value = Triple(
                surahs.getOrDefault(emptyList()),
                pages.getOrDefault(emptyList()),
                (surahs.exceptionOrNull() ?: pages.exceptionOrNull())?.let { it.message ?: "Nomaʼlum xatolik" },
            )
            loading.value = false
        }
    }

    fun setListView(view: String) {
        viewModelScope.launch { settingsRepository.setListView(view) }
    }

    fun setSurahOrder(order: String) {
        viewModelScope.launch { settingsRepository.setSurahOrder(order) }
    }

    fun setPageOrder(order: String) {
        viewModelScope.launch { settingsRepository.setPageOrder(order) }
    }

    fun togglePage(page: Int) {
        viewModelScope.launch { xatmRepository.toggle(page) }
    }

    /** Surani to'liq o'qildi deb belgilaydi yoki (hammasi belgilangan bo'lsa) belgilarni olib tashlaydi */
    fun toggleSurah(surahNumber: Int) {
        val range = JuzCatalog.surahPageRange(surahNumber) ?: return
        val done = range.all { state.value.xatm.containsKey(it) }
        /* allaqachon belgilangan betlarning sanasi saqlanib qoladi */
        viewModelScope.launch { xatmRepository.setRange(range.first, range.last, !done, keepExisting = true) }
    }
}
