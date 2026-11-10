package com.example.kotlin_holy.data.local

import android.content.Context
import androidx.collection.LruCache
import com.example.kotlin_holy.data.dto.AyahDto
import com.example.kotlin_holy.data.dto.MetaDto
import com.example.kotlin_holy.data.dto.MushafPageDto
import com.example.kotlin_holy.data.dto.PageContentDto
import com.example.kotlin_holy.data.dto.PageSummaryDto
import com.example.kotlin_holy.data.dto.SurahDetailDto
import com.example.kotlin_holy.data.dto.SurahDto
import com.example.kotlin_holy.data.dto.WordsResponseDto
import com.example.kotlin_holy.domain.model.Book
import com.example.kotlin_holy.domain.model.BookContent
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Barcha ma'lumot assets/data ichidagi JSON fayllardan o'qiladi (veb nusxadagi
 * public/data ning aynan nusxasi). Fayllar kichik bo'lgani uchun har biri
 * kerak bo'lganda o'qiladi va oxirgi o'qilganlari xotirada saqlanadi —
 * shunda betlarni varaqlash tez bo'ladi.
 */
@Singleton
class AssetJsonSource @Inject constructor(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /* Ochilgan betlar xotirada saqlanadi: varaqlaganda qaytadan o'qilmaydi */
    private val pageCache = LruCache<Int, PageContentDto>(12)
    private val mushafCache = LruCache<Int, MushafPageDto>(16)
    private val surahCache = LruCache<Int, SurahDetailDto>(4)
    private val hadithSectionCache = LruCache<String, List<Hadith>>(8)
    private val tafsirCache = LruCache<Int, Map<String, String>>(6)

    /* Kichik ro'yxatlar bir marta o'qiladi */
    private val listMutex = Mutex()
    private var surahList: List<SurahDto>? = null
    private var pageList: List<PageSummaryDto>? = null
    private var collections: List<HadithCollection>? = null
    private var books: List<Book>? = null
    private val sections = ConcurrentHashMap<String, List<HadithSection>>()
    private val bookContents = ConcurrentHashMap<String, BookContent>()

    private suspend inline fun <reified T> read(path: String): T = withContext(Dispatchers.IO) {
        context.assets.open(path).use { stream ->
            json.decodeFromString<T>(stream.readBytes().decodeToString())
        }
    }

    suspend fun surahs(): List<SurahDto> = listMutex.withLock {
        surahList ?: read<List<SurahDto>>("data/surahs.json").also { surahList = it }
    }

    suspend fun surah(number: Int): SurahDetailDto =
        surahCache[number] ?: read<SurahDetailDto>("data/surah/$number.json").also {
            surahCache.put(number, it)
        }

    suspend fun pages(): List<PageSummaryDto> = listMutex.withLock {
        pageList ?: read<List<PageSummaryDto>>("data/pages.json").also { pageList = it }
    }

    suspend fun page(number: Int): PageContentDto =
        pageCache[number] ?: read<PageContentDto>("data/page/$number.json").also {
            pageCache.put(number, it)
        }

    suspend fun mushafPage(number: Int): MushafPageDto =
        mushafCache[number] ?: read<MushafPageDto>("data/mushaf/$number.json").also {
            mushafCache.put(number, it)
        }

    suspend fun juz(number: Int): List<AyahDto> = read("data/juz/$number.json")

    suspend fun words(): WordsResponseDto = read("data/words.json")

    suspend fun meta(): MetaDto = read("data/meta.json")

    /** { "oyat raqami": "tafsir matni" } — tafsiri yo'q surada bo'sh */
    suspend fun tafsir(surahNumber: Int): Map<String, String> =
        tafsirCache[surahNumber] ?: runCatching { read<Map<String, String>>("data/tafsir/$surahNumber.json") }
            .getOrDefault(emptyMap())
            .also { tafsirCache.put(surahNumber, it) }

    /* ---------------- Hadislar ---------------- */

    suspend fun hadithCollections(): List<HadithCollection> = listMutex.withLock {
        collections ?: read<List<HadithCollection>>("data/hadith/collections.json").also { collections = it }
    }

    suspend fun hadithSections(collectionId: String): List<HadithSection> =
        sections[collectionId] ?: read<List<HadithSection>>("data/hadith/$collectionId/sections.json").also {
            sections[collectionId] = it
        }

    suspend fun hadiths(collectionId: String, sectionNumber: Int): List<Hadith> {
        val key = "$collectionId/$sectionNumber"
        return hadithSectionCache[key]
            ?: read<List<Hadith>>("data/hadith/$collectionId/section/$sectionNumber.json").also {
                hadithSectionCache.put(key, it)
            }
    }

    /* ---------------- Kitoblar ---------------- */

    suspend fun books(): List<Book> = listMutex.withLock {
        books ?: read<List<Book>>("data/books/books.json").also { books = it }
    }

    suspend fun bookContent(id: String): BookContent =
        bookContents[id] ?: read<BookContent>("data/books/$id.json").also { bookContents[id] = it }

    /* ---------------- Ma'lumot manbai ---------------- */

    /** assets/data ichidagi fayl baytlari — arxivga o'zgartirmasdan yozish uchun */
    suspend fun bytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        context.assets.open("data/$path").use { it.readBytes() }
    }

    /** manifest.json dagi fayllar ro'yxati */
    suspend fun manifestFiles(): List<String> = withContext(Dispatchers.IO) {
        val text = context.assets.open("data/manifest.json").use { it.readBytes().decodeToString() }
        val element = json.parseToJsonElement(text)
        val files = (element as? kotlinx.serialization.json.JsonObject)?.get("files")
            as? kotlinx.serialization.json.JsonArray ?: return@withContext emptyList()
        files.mapNotNull { file ->
            ((file as? kotlinx.serialization.json.JsonObject)?.get("path")
                as? kotlinx.serialization.json.JsonPrimitive)?.content
        }
    }
}
