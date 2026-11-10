package com.example.kotlin_holy.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Foydalanuvchi o'zi tanlagan eslatma — veb nusxadagi src/lib/reminders.js:
 * { id, type, title, text, source?, createdAt }.
 * `source` — oyatdan saqlanganda "2:255", hadisdan saqlanganda "hadith:bukhari:12".
 */
@Immutable
@Serializable
data class Reminder(
    val id: String,
    val type: String,
    val title: String = "",
    val text: String,
    val source: String? = null,
    val createdAt: Long = 0,
    /** Oyatlar mushaf shriftida chiqishi uchun: [{ surah, ayah, page }] */
    val verses: List<ReminderVerse> = emptyList(),
)

@Immutable
@Serializable
data class ReminderVerse(val surah: Int, val ayah: Int, val page: Int)

/** Eslatma turlari */
val REMINDER_TYPES = listOf("ayah" to "Oyat", "hadith" to "Hadis", "hikmat" to "Hikmat")

fun reminderTypeLabel(type: String): String =
    REMINDER_TYPES.firstOrNull { it.first == type }?.second ?: "Eslatma"

/** Hadisdan saqlangan eslatmaning manbasi: "hadith:bukhari:12" */
fun hadithReminderSource(collectionId: String, number: Double) =
    "hadith:$collectionId:${numberLabel(number)}"
