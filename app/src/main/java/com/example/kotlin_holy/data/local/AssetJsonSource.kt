package com.example.kotlin_holy.data.local

import android.content.Context
import androidx.collection.LruCache
import com.example.kotlin_holy.data.dto.AyahDto
import com.example.kotlin_holy.data.dto.MushafPageDto
import com.example.kotlin_holy.data.dto.PageContentDto
import com.example.kotlin_holy.data.dto.PageSummaryDto
import com.example.kotlin_holy.data.dto.SurahDetailDto
import com.example.kotlin_holy.data.dto.SurahDto
import com.example.kotlin_holy.data.dto.WordsResponseDto
import com.example.kotlin_holy.data.dto.MetaDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    private val mushafCache = LruCache<Int, MushafPageDto>(12)
    private val surahCache = LruCache<Int, SurahDetailDto>(4)

    private val surahListMutex = Mutex()
    private var surahList: List<SurahDto>? = null

    private suspend inline fun <reified T> read(path: String): T = withContext(Dispatchers.IO) {
        context.assets.open(path).use { stream ->
            json.decodeFromString<T>(stream.readBytes().decodeToString())
        }
    }

    suspend fun surahs(): List<SurahDto> = surahListMutex.withLock {
        surahList ?: read<List<SurahDto>>("data/surahs.json").also { surahList = it }
    }

    suspend fun surah(number: Int): SurahDetailDto =
        surahCache[number] ?: read<SurahDetailDto>("data/surah/$number.json").also {
            surahCache.put(number, it)
        }

    suspend fun pages(): List<PageSummaryDto> = read("data/pages.json")

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
}
