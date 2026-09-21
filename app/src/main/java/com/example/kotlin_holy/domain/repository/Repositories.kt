package com.example.kotlin_holy.domain.repository

import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.DataMeta
import com.example.kotlin_holy.domain.model.DifficultWord
import com.example.kotlin_holy.domain.model.MemorizationProgress
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.PageContent
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.model.SurahDetail
import com.example.kotlin_holy.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Qur'on matni va tarjimasi bilan ishlash */
interface QuranRepository {
    suspend fun surahs(): List<Surah>
    suspend fun surah(number: Int): SurahDetail
    suspend fun pages(): List<PageSummary>
    suspend fun page(number: Int): PageContent
    suspend fun mushafPage(number: Int): MushafPage
    suspend fun juzAyahs(number: Int): List<Ayah>
    suspend fun words(): List<DifficultWord>
    suspend fun meta(): DataMeta?
}

/** O'qilgan sahifalar (xatm) belgilari */
interface XatmRepository {
    /** Kalit — sahifa raqami, qiymat — belgilangan vaqt */
    val readPages: Flow<Map<Int, Long>>
    suspend fun toggle(page: Int)
    suspend fun setRange(from: Int, to: Int, read: Boolean, keepExisting: Boolean = false)
    suspend fun reset()
}

/** Oyatlarni ovoz bilan yodlash: har bir oyat uchun ketma-ket to'g'ri o'qishlar soni */
interface MemorizationRepository {
    /** Kalit — verseKey ("surah:ayah") */
    val progress: Flow<Map<String, MemorizationProgress>>
    suspend fun recordAttempt(verseKey: String, correct: Boolean)
    suspend fun reset(verseKey: String)
}

/** Ilova sozlamalari */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setArabicScale(scale: Float)
    suspend fun setTranslationScale(scale: Float)
    suspend fun setTextMode(mode: String)
    suspend fun setViewMode(mode: String)
    suspend fun setSurahOrder(order: String)
    suspend fun setPageOrder(order: String)
    suspend fun setLastRead(route: String?, label: String?)
    suspend fun setReciter(id: Int)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    /** "both" | "arabic" | "translation" */
    val textMode: String = "both",
    /** "ayah" | "page" */
    val viewMode: String = "ayah",
    /** "asc" | "desc" — ro'yxatlardagi tartib */
    val surahOrder: String = "asc",
    val pageOrder: String = "asc",
    /** Tanlangan qori — sura audiosi shu qori ovozida yuklanadi */
    val reciterId: Int = DEFAULT_RECITER_ID,
    val lastReadRoute: String? = null,
    val lastReadLabel: String? = null,
)

/** Mishari al-Afasiy — segmentlari to'liq tekshirilgan asosiy qori */
const val DEFAULT_RECITER_ID = 7
