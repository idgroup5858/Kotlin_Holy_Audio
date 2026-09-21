package com.example.kotlin_holy.data.mapper

import com.example.kotlin_holy.data.dto.AyahDto
import com.example.kotlin_holy.data.dto.MetaDto
import com.example.kotlin_holy.data.dto.MushafPageDto
import com.example.kotlin_holy.data.dto.PageContentDto
import com.example.kotlin_holy.data.dto.PageSummaryDto
import com.example.kotlin_holy.data.dto.SurahDetailDto
import com.example.kotlin_holy.data.dto.SurahDto
import com.example.kotlin_holy.data.dto.WordDto
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.DataMeta
import com.example.kotlin_holy.domain.model.DifficultWord
import com.example.kotlin_holy.domain.model.MushafLine
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.MushafWord
import com.example.kotlin_holy.domain.model.PageContent
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.model.SurahDetail

/* DTO → domen modellari. UI faqat domen modellari bilan ishlaydi. */

fun SurahDto.toDomain() = Surah(
    number = number,
    nameArabic = nameArabic,
    nameUz = nameUz,
    meaningUz = meaningUz,
    revelationPlace = revelationPlace,
    totalAyahs = totalAyahs,
    startPage = startPage,
)

fun SurahDetailDto.toDomain(): SurahDetail {
    val surah = Surah(
        number = number,
        nameArabic = nameArabic,
        nameUz = nameUz,
        meaningUz = meaningUz,
        revelationPlace = revelationPlace,
        totalAyahs = totalAyahs,
        startPage = startPage,
    )
    return SurahDetail(surah = surah, ayahs = ayahs.map { it.toDomain(number, nameUz) })
}

fun AyahDto.toDomain(fallbackSurah: Int? = null, fallbackName: String? = null) = Ayah(
    id = id,
    surahNumber = surah?.number ?: fallbackSurah ?: surahId ?: 0,
    numberInSurah = numberInSurah,
    textArabic = textArabic,
    translationUz = translationUz,
    transcription = transcription,
    tafsirUz = tafsirUz,
    pageNumber = pageNumber,
    juz = juz,
    surahNameUz = surah?.nameUz ?: fallbackName,
)

fun PageSummaryDto.toDomain() = PageSummary(
    number = number,
    ayahCount = ayahCount,
    hasTranslation = hasTranslation,
    juz = juz,
)

fun PageContentDto.toDomain() = PageContent(
    number = number,
    juz = juz ?: ayahs.firstOrNull()?.juz,
    note = note,
    ayahs = ayahs.map { it.toDomain() },
)

/**
 * Mushaf beti. Shu yerda har bir so'zga oyat ichidagi tartib raqami beriladi:
 * qiroat vaqt jadvali so'zlarni aynan shu raqam bo'yicha ko'rsatadi, shuning
 * uchun so'zga bosilganda yoki ijro paytida yonayotganda o'sha raqam ishlatiladi.
 * Oyat raqami glifi (charType == "end") so'z hisoblanmaydi va sanoqqa kirmaydi.
 */
fun MushafPageDto.toDomain(): MushafPage {
    val counters = HashMap<String, Int>()
    val mapped = lines.sortedBy { it.lineNumber }.map { line ->
        MushafLine(
            lineNumber = line.lineNumber,
            words = line.words.map { word ->
                val isEnd = word.charType == "end"
                val key = word.surahNumber.toString() + ":" + word.ayahNumber
                val number = if (isEnd) 0 else (counters[key] ?: 0) + 1
                if (!isEnd) counters[key] = number
                MushafWord(
                    code = word.code,
                    surahNumber = word.surahNumber,
                    ayahNumber = word.ayahNumber,
                    isEnd = isEnd,
                    wordNumber = number,
                )
            },
        )
    }
    return MushafPage(pageNumber = pageNumber, lines = mapped)
}

fun WordDto.toDomain() = DifficultWord(
    id = id,
    text = text,
    transliteration = transliteration,
    count = count,
    firstSurah = firstSurah,
    firstAyah = firstAyah,
)

fun MetaDto.toDomain() = DataMeta(
    surahs = surahs,
    ayahs = ayahs,
    pages = pages,
    mushafPages = mushafPages,
    words = words,
    files = files,
    totalBytes = totalBytes,
)
