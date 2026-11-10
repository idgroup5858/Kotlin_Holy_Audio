package com.example.kotlin_holy.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.RemindersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Bosh sahifadagi doimiy oyat tabi: mushaf betidagi o'rni bilan */
data class Excerpt(
    val id: String,
    val label: String,
    val surah: Int,
    val ayahs: List<Int>,
    val pageNumber: Int,
    val caption: String,
)

/**
 * Sura qidiruvi ustidagi tablar. Birinchisi — tasodifiy eslatma; qolganlari doimiy oyatlar
 * (eslatma emas): mushaf betidagi o'rni bilan, shu tabning o'zida ochiladi.
 */
val EXCERPTS = listOf(
    Excerpt("kursi", "Oyatal Kursiy", 2, listOf(255), 42, "Baqara surasi, 255-oyat · 42-sahifa"),
    Excerpt("omanar", "Aamanar-Rasulu", 2, listOf(285, 286), 49, "Baqara surasi, 285–286-oyatlar · 49-sahifa"),
)

data class ExcerptData(val mushaf: MushafPage?, val ayahs: List<Ayah>)

/** Dastur har ochilganda bitta tasodifiy eslatma tanlanadi va shu seans davomida o'zgarmaydi */
object SessionReminder {
    private var pickedId: String? = null

    fun pick(list: List<Reminder>): Reminder? {
        if (list.isEmpty()) return null
        val picked = list.firstOrNull { it.id == pickedId } ?: list.random()
        pickedId = picked.id
        return picked
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    remindersRepository: RemindersRepository,
    private val ayahSaver: AyahSaver,
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>?> =
        remindersRepository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val saved: StateFlow<Set<String>> =
        ayahSaver.savedVerses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private val _excerpts = MutableStateFlow<Map<String, ExcerptData>>(emptyMap())
    val excerpts: StateFlow<Map<String, ExcerptData>> = _excerpts

    private val _tafsir = MutableStateFlow<Map<String, String>>(emptyMap())
    val tafsir: StateFlow<Map<String, String>> = _tafsir

    /** Tab ochilganda betning mushaf joylashuvi va tarjimasi yuklanadi */
    fun loadExcerpt(excerpt: Excerpt) {
        if (_excerpts.value.containsKey(excerpt.id)) return
        viewModelScope.launch {
            val mushaf = runCatching { quranRepository.mushafPage(excerpt.pageNumber) }.getOrNull()
            val ayahs = runCatching { quranRepository.page(excerpt.pageNumber).ayahs }.getOrDefault(emptyList())
                .filter { it.surahNumber == excerpt.surah && it.numberInSurah in excerpt.ayahs }
            _excerpts.value = _excerpts.value + (excerpt.id to ExcerptData(mushaf, ayahs))
            if (_tafsir.value.isEmpty()) {
                _tafsir.value = runCatching { quranRepository.tafsir(excerpt.surah) }.getOrDefault(emptyMap())
                    .mapKeys { (ayah, _) -> "${excerpt.surah}:$ayah" }
            }
        }
    }

    fun toggleSave(ayah: Ayah) {
        viewModelScope.launch { ayahSaver.toggle(ayah) }
    }
}
