package com.example.kotlin_holy.domain.model

import androidx.compose.runtime.Immutable

/**
 * Qiroat qiluvchi qori.
 *
 * Har bir sura bitta yaxlit mp3 fayl bo'lib keladi, unga oyat va so'z
 * chegaralarini ko'rsatuvchi vaqt jadvali qo'shiladi. Shu sababli bir surani
 * yuklash — ikki fayl: audio va jadval.
 */
@Immutable
data class Reciter(
    val id: Int,
    val nameUz: String,
    /** "Murattal" yoki "Mujawwad" — qiroat uslubi */
    val style: String,
) {
    val label: String get() = "$nameUz · $style"
}

/** Oyatning sura faylidagi o'rni */
@Immutable
data class AyahTiming(
    val surahNumber: Int,
    val ayahNumber: Int,
    val startMs: Long,
    val endMs: Long,
    val words: List<WordSegment>,
    /**
     * Vaqt jadvali so'zlarni mushafdan boshqacha raqamlagan bo'lsa (so'z raqami
     * mushafdagi so'zlar sonidan oshsa), so'zma-so'z yonish noto'g'ri so'zni
     * belgilaydi — shuning uchun bunday oyatlarda faqat oyat darajasida yonadi.
     */
    val wordsReliable: Boolean,
) {
    val verseKey: String get() = "$surahNumber:$ayahNumber"

    /** Berilgan vaqtda qaysi so'z o'qilayotganini topadi, topilmasa null */
    fun wordAt(positionMs: Long): Int? {
        if (!wordsReliable) return null
        val word = words.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }
        return word?.position
    }
}

/** So'zning boshlanish va tugash vaqti (sura fayli boshidan, millisekundda) */
@Immutable
data class WordSegment(
    val position: Int,
    val startMs: Long,
    val endMs: Long,
)

/** Bir suraning to'liq audio to'plami */
@Immutable
data class SurahAudio(
    val surahNumber: Int,
    val reciterId: Int,
    val timings: List<AyahTiming>,
) {
    private val byVerse: Map<String, AyahTiming> = timings.associateBy { it.verseKey }

    fun timing(ayahNumber: Int): AyahTiming? = byVerse["$surahNumber:$ayahNumber"]

    /** Berilgan vaqtda qaysi oyat o'qilayotgani */
    fun ayahAt(positionMs: Long): AyahTiming? =
        timings.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs }
}

/** Suraning qurilmadagi holati */
@Immutable
sealed interface AudioDownload {
    /** Hali yuklanmagan; hajmi oldindan so'ralganda ma'lum bo'ladi */
    data class Absent(val expectedBytes: Long? = null) : AudioDownload

    data class InProgress(val downloadedBytes: Long, val totalBytes: Long?) : AudioDownload {
        val percent: Int
            get() = totalBytes?.takeIf { it > 0 }
                ?.let { ((downloadedBytes * 100) / it).toInt().coerceIn(0, 100) } ?: 0
    }

    data class Ready(val bytes: Long) : AudioDownload

    data class Failed(val reason: String?) : AudioDownload
}
