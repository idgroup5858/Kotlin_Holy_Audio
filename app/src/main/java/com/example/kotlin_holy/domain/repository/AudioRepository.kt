package com.example.kotlin_holy.domain.repository

import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.domain.model.SurahAudio
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Qiroat bilan ishlash.
 *
 * Audio butun Qur'on uchun emas, faqat ochilgan sura uchun yuklanadi: bir sura —
 * bitta mp3 va bitta vaqt jadvali. Yuklangani qurilmada qoladi va keyin
 * internetsiz ishlatiladi, foydalanuvchi xohlasa o'chirib, joyni bo'shatadi.
 */
interface AudioRepository {

    /** Har bir suraning hozirgi holati: yuklanmagan / yuklanmoqda / tayyor */
    fun downloadState(surahNumber: Int): Flow<AudioDownload>

    /** Barcha suralar holati — sozlamalardagi xotira ro'yxati uchun */
    val downloads: Flow<Map<Int, AudioDownload>>

    /** Yuklangan suraning vaqt jadvali; yuklanmagan bo'lsa null */
    suspend fun audio(surahNumber: Int): SurahAudio?

    /** Ijro uchun mahalliy fayl; yuklanmagan bo'lsa null */
    fun audioFile(surahNumber: Int): File?

    /** Yuklashdan oldin hajmni bilish uchun */
    suspend fun expectedSize(surahNumber: Int): Long?

    suspend fun download(surahNumber: Int)
    fun cancel(surahNumber: Int)
    suspend fun delete(surahNumber: Int)
    suspend fun deleteAll()

    /** Egallangan umumiy joy */
    suspend fun storageBytes(): Long
    suspend fun downloadedSurahs(): List<Int>
}
