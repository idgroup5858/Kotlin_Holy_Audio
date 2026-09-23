package com.example.kotlin_holy.ui.feature.surahs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.MushafPage
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

/**
 * Bosh sahifada ko'rsatiladigan tanlangan oyatlar to'plami. Oyat al-Kursiy va
 * Amanar-Rasul uchun mushaf betlari ham saqlanadi — arabcha matn aynan
 * bosma mushaf sahifasidagi shrift va joylashuv bilan chiziladi.
 */
data class HighlightAyahs(
    val anbiyo: List<Ayah> = emptyList(),
    val kursiy: List<Ayah> = emptyList(),
    val kursiyPages: List<MushafPage> = emptyList(),
    val amanarrasul: List<Ayah> = emptyList(),
    val amanarrasulPages: List<MushafPage> = emptyList(),
    val zuxruf: List<Ayah> = emptyList(),
)

/** Suraning xatm holati betlar belgisidan hisoblanadi — alohida saqlanmaydi */
data class SurahXatm(val read: Int, val total: Int) {
    val done: Boolean get() = total > 0 && read == total
    val started: Boolean get() = read > 0
}

data class SurahListState(
    val loading: Boolean = true,
    val surahs: List<Surah> = emptyList(),
    val query: String = "",
    val order: String = "asc",
    val xatm: Map<Int, SurahXatm> = emptyMap(),
) {
    val visible: List<Surah>
        get() {
            val sorted = if (order == "desc") surahs.sortedByDescending { it.number } else surahs
            val q = query.trim().lowercase()
            if (q.isEmpty()) return sorted
            return sorted.filter { surah ->
                surah.number.toString().contains(q) ||
                    surah.nameUz.lowercase().contains(q) ||
                    (surah.meaningUz?.lowercase()?.contains(q) == true) ||
                    surah.nameArabic.contains(query.trim())
            }
        }
}

@HiltViewModel
class SurahListViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val surahs = MutableStateFlow<List<Surah>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val query = MutableStateFlow("")

    private val _highlightAyahs = MutableStateFlow(HighlightAyahs())
    val highlightAyahs: StateFlow<HighlightAyahs> = _highlightAyahs

    private val _highlightTab = MutableStateFlow("anbiyo")
    val highlightTab: StateFlow<String> = _highlightTab

    fun selectHighlightTab(key: String) {
        _highlightTab.value = key
    }

    val state: StateFlow<SurahListState> = combine(
        surahs,
        loading,
        query,
        xatmRepository.readPages,
        settingsRepository.settings,
    ) { list, isLoading, q, pages, settings ->
        SurahListState(
            loading = isLoading,
            surahs = list,
            query = q,
            order = settings.surahOrder,
            xatm = list.associate { surah -> surah.number to surah.xatm(pages) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurahListState())

    init {
        viewModelScope.launch {
            surahs.value = runCatching { quranRepository.surahs() }.getOrDefault(emptyList())
            loading.value = false
        }
        viewModelScope.launch {
            val baqara = runCatching { quranRepository.surah(2) }.getOrNull()?.ayahs.orEmpty()
            val anbiyo = runCatching { quranRepository.surah(21) }.getOrNull()?.ayahs.orEmpty()
            val zuxruf = runCatching { quranRepository.surah(43) }.getOrNull()?.ayahs.orEmpty()

            val kursiyAyahs = baqara.filter { it.numberInSurah == 255 }
            val amanarrasulAyahs = baqara.filter { it.numberInSurah in 285..286 }

            _highlightAyahs.value = HighlightAyahs(
                anbiyo = anbiyo.filter { it.numberInSurah in 1..2 },
                kursiy = kursiyAyahs,
                kursiyPages = kursiyAyahs.mushafPages(),
                amanarrasul = amanarrasulAyahs,
                amanarrasulPages = amanarrasulAyahs.mushafPages(),
                zuxruf = zuxruf.filter { it.numberInSurah == 43 },
            )
        }
    }

    /** Berilgan oyatlar joylashgan mushaf betlarini (takrorsiz) yuklaydi */
    private suspend fun List<Ayah>.mushafPages(): List<MushafPage> {
        val pageNumbers = mapNotNull { it.pageNumber }.distinct()
        return pageNumbers.mapNotNull { runCatching { quranRepository.mushafPage(it) }.getOrNull() }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    /** «O'sish → Kamayish» va aksincha */
    fun toggleOrder() {
        val next = if (state.value.order == "asc") "desc" else "asc"
        viewModelScope.launch { settingsRepository.setSurahOrder(next) }
    }

    fun toggleSurah(surahNumber: Int) {
        val range = JuzCatalog.surahPageRange(surahNumber) ?: return
        val done = state.value.xatm[surahNumber]?.done == true
        viewModelScope.launch {
            xatmRepository.setRange(range.first, range.last, read = !done, keepExisting = true)
        }
    }
}

private fun Surah.xatm(pages: Map<Int, Long>): SurahXatm {
    val range = JuzCatalog.surahPageRange(number) ?: return SurahXatm(0, 0)
    val read = range.count { pages.containsKey(it) }
    return SurahXatm(read = read, total = range.count())
}
