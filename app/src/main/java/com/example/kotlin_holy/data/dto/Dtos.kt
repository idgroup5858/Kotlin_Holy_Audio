package com.example.kotlin_holy.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// JSON fayllarning tuzilishi — veb nusxadagi public/data bilan bir xil

@Serializable
data class SurahDto(
    val number: Int,
    val nameArabic: String,
    val nameUz: String,
    val meaningUz: String? = null,
    val revelationPlace: String? = null,
    val totalAyahs: Int = 0,
    val startPage: Int = 1,
)

@Serializable
data class AyahDto(
    val id: Int,
    val numberInSurah: Int,
    val textArabic: String = "",
    val translationUz: String? = null,
    val transcription: String? = null,
    val tafsirUz: String? = null,
    val pageNumber: Int? = null,
    val juz: Int? = null,
    val surahId: Int? = null,
    // page va juz fayllarida oyat o'z surasi bilan birga keladi
    val surah: SurahDto? = null,
)

@Serializable
data class SurahDetailDto(
    val number: Int,
    val nameArabic: String,
    val nameUz: String,
    val meaningUz: String? = null,
    val revelationPlace: String? = null,
    val totalAyahs: Int = 0,
    val startPage: Int = 1,
    val ayahs: List<AyahDto> = emptyList(),
)

@Serializable
data class PageSummaryDto(
    val number: Int,
    val ayahCount: Int = 0,
    val hasTranslation: Boolean = false,
    val juz: Int? = null,
)

@Serializable
data class PageContentDto(
    val number: Int,
    val juz: Int? = null,
    val note: String? = null,
    val ayahs: List<AyahDto> = emptyList(),
)

@Serializable
data class MushafWordDto(
    val code: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val charType: String = "word",
)

@Serializable
data class MushafLineDto(
    val lineNumber: Int,
    val words: List<MushafWordDto> = emptyList(),
)

@Serializable
data class MushafPageDto(
    val pageNumber: Int,
    val lines: List<MushafLineDto> = emptyList(),
)

@Serializable
data class WordDto(
    val id: Int,
    val text: String,
    val textPlain: String? = null,
    val transliteration: String? = null,
    val count: Int = 0,
    val firstSurah: Int? = null,
    val firstAyah: Int? = null,
)

@Serializable
data class WordsResponseDto(
    val total: Int = 0,
    val items: List<WordDto> = emptyList(),
)

@Serializable
data class MetaDto(
    val surahs: Int = 0,
    val ayahs: Int = 0,
    val pages: Int = 0,
    val mushafPages: Int = 0,
    val words: Int = 0,
    val files: Int = 0,
    @SerialName("totalBytes") val totalBytes: Long = 0,
)
