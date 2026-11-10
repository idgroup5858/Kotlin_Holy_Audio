package com.example.kotlin_holy.ui.feature.home

import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.RemindersRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Oyat tarjimasini «Eslatmalar»ga saqlash (faqat o'zbekcha matn, arabchasiz) — veb nusxadagi
 * AyahReminderButton. Saqlangan oyat qayta bosilsa eslatmalardan olib tashlanadi.
 */
@Singleton
class AyahSaver @Inject constructor(
    private val reminders: RemindersRepository,
    private val quran: QuranRepository,
) {
    /** Eslatmalarga saqlangan oyatlar: "2:255" */
    val savedVerses: Flow<Set<String>> = reminders.reminders.map { list ->
        list.mapNotNull { it.source }.filter { !it.startsWith("hadith:") }.toSet()
    }

    suspend fun toggle(ayah: Ayah, current: List<Reminder>? = null) {
        val source = ayah.verseKey
        val list = current ?: reminderList()
        val saved = list.firstOrNull { it.source == source }
        if (saved != null) {
            reminders.delete(saved.id)
            return
        }
        val name = ayah.surahNameUz
            ?: runCatching { quran.surahs().firstOrNull { it.number == ayah.surahNumber }?.nameUz }.getOrNull()
        reminders.add(
            type = "ayah",
            title = if (name != null) "$name surasi ${ayah.numberInSurah}-oyat" else "${ayah.surahNumber}-sura ${ayah.numberInSurah}-oyat",
            text = "${ayah.numberInSurah}. ${ayah.translationUz.orEmpty()}",
            source = source,
        )
    }

    private suspend fun reminderList(): List<Reminder> = reminders.reminders.first()
}
