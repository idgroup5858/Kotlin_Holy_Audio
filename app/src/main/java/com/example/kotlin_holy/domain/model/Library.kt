package com.example.kotlin_holy.domain.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * Hadis to'plamlari va kitoblar — veb nusxadagi public/data/hadith va public/data/books
 * fayllarining tuzilishi. Fayllar o'zgarmaydi, shuning uchun JSON to'g'ridan-to'g'ri
 * shu modellarga o'qiladi.
 */

/** Hadis kitobi (to'plami): hadith/collections.json */
@Immutable
@Serializable
data class HadithCollection(
    val id: String,
    val nameArabic: String = "",
    val nameUz: String = "",
    val authorUz: String = "",
    val descriptionUz: String? = null,
    val totalHadiths: Int = 0,
    val translatedHadiths: Int = 0,
    val sections: Int = 0,
)

/** Kitobning bo'limi: hadith/<id>/sections.json */
@Immutable
@Serializable
data class HadithSection(
    val number: Int,
    val nameUz: String? = null,
    val nameEn: String? = null,
    val firstHadith: Double = 0.0,
    val lastHadith: Double = 0.0,
    val totalHadiths: Int = 0,
    val translatedHadiths: Int = 0,
    /** Bo'lim hadislari oralig'i — Termiziyda bo'limlar aralash, shuning uchun bir nechta */
    val ranges: List<List<Double>>? = null,
) {
    /** Bo'lim nomi: o'zbekcha bo'lmasa inglizcha, u ham bo'lmasa raqam */
    val title: String get() = nameUz ?: nameEn ?: "$number-boʻlim"

    val hadithRanges: List<ClosedFloatingPointRange<Double>>
        get() = ranges?.mapNotNull { if (it.size >= 2) it[0]..it[1] else null }
            ?: listOf(firstHadith..lastHadith)
}

/** Bitta hadis: hadith/<id>/section/<n>.json. Raqam kasr bo'lishi mumkin (Termiziy 1114.2) */
@Immutable
@Serializable
data class Hadith(
    val number: Double,
    val numberTo: Double? = null,
    val textArabic: String? = null,
    val narratorUz: String? = null,
    val textUz: String? = null,
    val sourceUz: String? = null,
    val grade: String? = null,
    /** "ai" — tarjima sun'iy intellekt yordamida qilingan */
    val translation: String? = null,
) {
    val label: String get() = numberLabel(number)

    /** «12-hadis» yoki «12–14-hadis» */
    val rangeLabel: String
        get() = if (numberTo != null) "${numberLabel(number)}–${numberLabel(numberTo)}" else numberLabel(number)

    /** Hadis raqami shu hadisga tegishlimi (birlashtirilgan hadislar uchun oraliq) */
    fun matches(value: Double): Boolean =
        value == number || (numberTo != null && value >= number && value <= numberTo)
}

/** JavaScript'dagi String(number) kabi: 12.0 → "12", 1114.2 → "1114.2" */
fun numberLabel(value: Double): String =
    if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()

/* ---------------- Kitoblar (hadis to'plamlaridan tashqari) ---------------- */

/** books/books.json */
@Immutable
@Serializable
data class Book(
    val id: String,
    val nameUz: String = "",
    val nameArabic: String = "",
    val subtitleUz: String? = null,
    val authorUz: String = "",
    val descriptionUz: String? = null,
    val noteUz: String? = null,
    /** «hikmat», «bob» */
    val itemName: String = "bob",
    val itemsTitleUz: String? = null,
    val totalItems: Int = 0,
) {
    /** «12-hikmat» */
    fun itemLabel(number: Int) = "$number-$itemName"
}

/** books/<id>.json */
@Immutable
@Serializable
data class BookContent(
    val pages: List<BookPage> = emptyList(),
    val items: List<BookItem> = emptyList(),
)

/** Muqaddima, Atamalar lug'ati kabi alohida sahifa */
@Immutable
@Serializable
data class BookPage(
    val slug: String,
    val title: String = "",
    val summaryUz: String? = null,
    val blocks: List<BookBlock> = emptyList(),
)

/**
 * Kitob bo'lagi: qisqa (hikmat — text, commentary) yoki bob (title, summaryUz, blocks).
 */
@Immutable
@Serializable
data class BookItem(
    val number: Int,
    val text: String? = null,
    val commentary: String? = null,
    val title: String? = null,
    val subtitleUz: String? = null,
    val summaryUz: String? = null,
    val blocks: List<BookBlock>? = null,
)

/**
 * Matn bloki: { type: section | h | p | quote | arabic, text },
 * { type: list, ordered?, start?, items } — band matn yoki { text, items },
 * { type: terms, items: [{ term, text }] }.
 */
@Immutable
@Serializable
data class BookBlock(
    val type: String = "p",
    val text: String? = null,
    val ordered: Boolean = false,
    val start: Int? = null,
    val items: List<JsonElement>? = null,
)
