package com.example.kotlin_holy.data.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Sura audiosi haqidagi javob tuzilishi.
 *
 * segments — har biri [so'z_raqami, boshlanish_ms, tugash_ms] ko'rinishida.
 * Ammo ba'zi yozuvlarda tugash vaqti tushib qolgan va massiv ikki elementli
 * bo'ladi; bunda tugash vaqti keyingi so'zning boshlanishidan tiklanadi.
 */

@Serializable
data class ChapterAudioResponseDto(
    @SerialName("audio_file") val audioFile: ChapterAudioFileDto? = null,
)

@Serializable
data class ChapterAudioFileDto(
    @SerialName("chapter_id") val chapterId: Int = 0,
    /* Ba'zi qorilarda 0 kelib qoladi — shuning uchun haqiqiy hajm
       yuklashdan oldin Content-Length sarlavhasidan olinadi */
    @SerialName("file_size") val fileSize: Double = 0.0,
    @SerialName("audio_url") val audioUrl: String = "",
    val timestamps: List<AyahTimestampDto> = emptyList(),
)

@Serializable
data class AyahTimestampDto(
    @SerialName("verse_key") val verseKey: String = "",
    @SerialName("timestamp_from") val from: Long = 0,
    @SerialName("timestamp_to") val to: Long = 0,
    val segments: List<List<Long>> = emptyList(),
)

/** Yuklab olingan jadval qurilmada shu ko'rinishda saqlanadi */
@Serializable
data class StoredTimingsDto(
    val surahNumber: Int,
    val reciterId: Int,
    val audioUrl: String,
    val ayahs: List<StoredAyahDto>,
)

@Serializable
data class StoredAyahDto(
    val ayah: Int,
    val from: Long,
    val to: Long,
    /** So'z chegaralari tekshiruvdan o'tgan bo'lsa true */
    val reliable: Boolean,
    /** Tekislangan uchlik: [raqam, boshlanish, tugash, raqam, ...] */
    val words: List<Long>,
)
