package com.example.kotlin_holy.domain.repository

import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.Book
import com.example.kotlin_holy.domain.model.BookContent
import com.example.kotlin_holy.domain.model.DataMeta
import com.example.kotlin_holy.domain.model.DifficultWord
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.PageContent
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.model.SurahDetail
import com.example.kotlin_holy.ui.theme.Appearance
import com.example.kotlin_holy.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow

/** Qur'on matni, tarjimasi va tafsiri bilan ishlash */
interface QuranRepository {
    suspend fun surahs(): List<Surah>
    suspend fun surah(number: Int): SurahDetail
    suspend fun pages(): List<PageSummary>
    suspend fun page(number: Int): PageContent
    suspend fun mushafPage(number: Int): MushafPage
    suspend fun juzAyahs(number: Int): List<Ayah>
    suspend fun words(): List<DifficultWord>
    suspend fun meta(): DataMeta?

    /** Oyat raqami → tafsir matni; tafsiri yo'q surada bo'sh */
    suspend fun tafsir(surahNumber: Int): Map<Int, String>
}

/** Hadis to'plamlari va kitoblar */
interface LibraryRepository {
    suspend fun collections(): List<HadithCollection>
    suspend fun collection(id: String): HadithCollection?
    suspend fun sections(collectionId: String): List<HadithSection>
    suspend fun hadiths(collectionId: String, sectionNumber: Int): List<Hadith>
    suspend fun books(): List<Book>
    suspend fun book(id: String): Book?
    suspend fun bookContent(id: String): BookContent
}

/** O'qilgan sahifalar (xatm) belgilari */
interface XatmRepository {
    /** Kalit — sahifa raqami, qiymat — belgilangan vaqt */
    val readPages: Flow<Map<Int, Long>>

    /** Xatm boshlangan vaqt — birinchi bet belgilanganda yoziladi, tozalanganda o'chadi */
    val startedAt: Flow<Long?>
    suspend fun toggle(page: Int)
    suspend fun setRange(from: Int, to: Int, read: Boolean, keepExisting: Boolean = false)
    suspend fun reset()
}

/** «O'qildi» belgilari: hadislar ("bukhari:1") va kitob bo'laklari ("hikam:12") */
interface MarksRepository {
    val hadithMarks: Flow<Map<String, Long>>
    val bookMarks: Flow<Map<String, Long>>
    suspend fun toggleHadith(collectionId: String, number: Double)
    suspend fun setHadiths(collectionId: String, numbers: List<Double>, read: Boolean)
    suspend fun toggleBookItem(bookId: String, number: Int)
}

/** Bosh sahifadagi «Bugungi eslatma» uchun oyat, hadis va hikmatlar */
interface RemindersRepository {
    val reminders: Flow<List<Reminder>>
    suspend fun add(type: String, title: String, text: String, source: String? = null)
    suspend fun delete(id: String)
}

/** O'qish sahifalarida kunlar bo'yicha o'tkazilgan vaqt, ms: "2026-10-06" → 4380000 */
interface ReadingTimeRepository {
    val totals: Flow<Map<String, Long>>
    suspend fun add(amounts: Map<String, Long>)
    suspend fun reset()
}

/** Ilova sozlamalari */
interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAppearance(appearance: Appearance)
    suspend fun setArabicScale(scale: Float)
    suspend fun setTranslationScale(scale: Float)
    suspend fun setTextMode(mode: String)
    suspend fun setViewMode(mode: String)
    suspend fun setListView(view: String)
    suspend fun setSurahOrder(order: String)
    suspend fun setPageOrder(order: String)
    suspend fun setLastRead(route: String?, label: String?)
    suspend fun setReciter(id: Int)
}

data class AppSettings(
    /** Veb nusxadagidek standart — qorong'i */
    val themeMode: ThemeMode = ThemeMode.DARK,
    val appearance: Appearance = Appearance(),
    val arabicScale: Float = 1f,
    val translationScale: Float = 1f,
    /** "both" | "arabic" | "translation" */
    val textMode: String = "both",
    /** "ayah" | "page" */
    val viewMode: String = "ayah",
    /** "grid" | "list" — barcha ro'yxat sahifalari uchun umumiy */
    val listView: String = "grid",
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
