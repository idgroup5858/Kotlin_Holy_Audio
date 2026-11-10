package com.example.kotlin_holy.data.audio

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

/*
 * Sura audiosi haqidagi javob tuzilishi.
 *
 * segments — har biri [so'z_raqami, boshlanish_ms, tugash_ms] ko'rinishida.
 * Ammo ba'zi yozuvlarda tugash vaqti tushib qolgan va massiv ikki elementli
 * bo'ladi; bunda tugash vaqti keyingi so'zning boshlanishidan tiklanadi.
 *
 * MUHIM: vaqtlar ko'p qorilarda butun son emas, "2300.0" yoki
 * "16219.999999999998" ko'rinishida keladi (Kahf, Baqara va boshqalar).
 * Long sifatida o'qilsa JSON xatosi chiqib, sura umuman yuklanmay qolardi —
 * shuning uchun barcha vaqtlar Double qabul qilinadi va keyin yaxlitlanadi.
 */

@Serializable
data class ChapterAudioResponseDto(
    @SerialName("audio_file") val audioFile: ChapterAudioFileDto? = null,
)

@Serializable
data class ChapterAudioFileDto(
    @SerialName("chapter_id") val chapterId: Int = 0,
    /* Ba'zi qorilarda 0 yoki null kelib qoladi — shuning uchun haqiqiy hajm
       yuklashdan oldin Content-Length sarlavhasidan olinadi */
    @SerialName("file_size") val fileSize: Double = 0.0,
    @SerialName("audio_url") val audioUrl: String = "",
    val timestamps: List<AyahTimestampDto> = emptyList(),
)

@Serializable
data class AyahTimestampDto(
    @SerialName("verse_key") val verseKey: String = "",
    @SerialName("timestamp_from") val from: Double = 0.0,
    @SerialName("timestamp_to") val to: Double = 0.0,
    val segments: List<List<Double>> = emptyList(),
)

/** API vaqtini millisekundga: 16219.999999999998 → 16220 */
internal fun Double.toMillis(): Long = roundToLong()

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
