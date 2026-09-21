package com.example.kotlin_holy.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class Surah(
    val number: Int,
    val nameArabic: String,
    val nameUz: String,
    val meaningUz: String?,
    val revelationPlace: String?,
    val totalAyahs: Int,
    val startPage: Int,
) {
    val isMakki: Boolean get() = revelationPlace == "makka"
}

@Immutable
data class Ayah(
    val id: Int,
    val surahNumber: Int,
    val numberInSurah: Int,
    val textArabic: String,
    val translationUz: String?,
    val transcription: String?,
    val tafsirUz: String?,
    val pageNumber: Int?,
    val juz: Int?,
    val surahNameUz: String? = null,
) {
    val verseKey: String get() = "$surahNumber:$numberInSurah"
}

/** Sura sahifasi: sura ma'lumoti va uning barcha oyatlari */
@Immutable
data class SurahDetail(
    val surah: Surah,
    val ayahs: List<Ayah>,
)

/** Sahifalar ro'yxatidagi qatorlar */
@Immutable
data class PageSummary(
    val number: Int,
    val ayahCount: Int,
    val hasTranslation: Boolean,
    val juz: Int?,
)

/** Bitta mushaf beti: oyatlari va tarjimasi */
@Immutable
data class PageContent(
    val number: Int,
    val juz: Int?,
    val note: String?,
    val ayahs: List<Ayah>,
)

/* ---------- Mushaf betining glif tartibi ---------- */

@Immutable
data class MushafWord(
    val code: String,
    val surahNumber: Int,
    val ayahNumber: Int,
    val isEnd: Boolean,
    /**
     * So'zning oyat ichidagi tartib raqami, 1 dan boshlanadi. Oyat oxiridagi
     * raqam glifida 0 bo'ladi, chunki u so'z emas. Qiroatning vaqt jadvalidagi
     * so'z raqami aynan shu songa to'g'ri keladi — tekshiruvda mushafdagi
     * so'zlar soni audio fayllari bilan to'liq mos chiqqan.
     */
    val wordNumber: Int = 0,
) {
    val verseKey: String get() = "$surahNumber:$ayahNumber"
}

@Immutable
data class MushafLine(
    val lineNumber: Int,
    val words: List<MushafWord>,
)

@Immutable
data class MushafPage(
    val pageNumber: Int,
    val lines: List<MushafLine>,
)

/** Qiyin so'zlar lug'ati */
@Immutable
data class DifficultWord(
    val id: Int,
    val text: String,
    val transliteration: String?,
    val count: Int,
    val firstSurah: Int?,
    val firstAyah: Int?,
)

/** Juz chegaralari — Qur'on o'zgarmagani uchun doimiy ro'yxatdan olinadi */
@Immutable
data class JuzInfo(
    val number: Int,
    val fromSurah: Int,
    val fromAyah: Int,
    val toSurah: Int,
    val toAyah: Int,
    val fromPage: Int,
    val toPage: Int,
    val ayahCount: Int,
)

/** Ma'lumot fayllari haqida qisqacha ma'lumot (sozlamalarda ko'rsatiladi) */
@Immutable
data class DataMeta(
    val surahs: Int,
    val ayahs: Int,
    val pages: Int,
    val mushafPages: Int,
    val words: Int,
    val files: Int,
    val totalBytes: Long,
)
