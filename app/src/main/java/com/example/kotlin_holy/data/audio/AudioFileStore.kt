package com.example.kotlin_holy.data.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Yuklab olingan qiroat fayllari qurilmaning ilovaga ajratilgan papkasida turadi:
 *
 *   .../files/audio/<qori>/<sura>.mp3    — suraning yaxlit qiroati
 *   .../files/audio/<qori>/<sura>.json   — oyat va so'z vaqtlari
 *
 * Bu papka uchun alohida ruxsat kerak emas, ilova o'chirilsa fayllar ham
 * o'chadi va foydalanuvchi tizim sozlamalaridan ham tozalay oladi.
 *
 * Yuklash tugamaguncha fayl ".part" kengaytmasi bilan yoziladi — yarim yuklangan
 * fayl hech qachon tayyor deb hisoblanmaydi.
 */
@Singleton
class AudioFileStore @Inject constructor(
    private val context: Context,
) {

    private val root: File
        get() = File(context.getExternalFilesDir(null) ?: context.filesDir, "audio")

    private fun reciterDir(reciterId: Int) = File(root, reciterId.toString())

    fun audioFile(reciterId: Int, surahNumber: Int) =
        File(reciterDir(reciterId), "$surahNumber.mp3")

    fun partFile(reciterId: Int, surahNumber: Int) =
        File(reciterDir(reciterId), "$surahNumber.mp3.part")

    fun timingsFile(reciterId: Int, surahNumber: Int) =
        File(reciterDir(reciterId), "$surahNumber.json")

    /** Sura to'liq yuklanganmi: audio ham, jadval ham joyida bo'lishi shart */
    fun isReady(reciterId: Int, surahNumber: Int): Boolean {
        val audio = audioFile(reciterId, surahNumber)
        val timings = timingsFile(reciterId, surahNumber)
        return audio.isFile && audio.length() > 0 && timings.isFile && timings.length() > 0
    }

    fun ensureDir(reciterId: Int) {
        reciterDir(reciterId).mkdirs()
    }

    /** Shu qori uchun yuklangan suralar raqamlari */
    suspend fun downloadedSurahs(reciterId: Int): List<Int> = withContext(Dispatchers.IO) {
        reciterDir(reciterId).listFiles()
            ?.mapNotNull { file ->
                file.name.removeSuffix(".mp3").toIntOrNull()
                    ?.takeIf { file.name.endsWith(".mp3") && isReady(reciterId, it) }
            }
            ?.sorted()
            .orEmpty()
    }

    suspend fun sizeOf(reciterId: Int, surahNumber: Int): Long = withContext(Dispatchers.IO) {
        audioFile(reciterId, surahNumber).length() + timingsFile(reciterId, surahNumber).length()
    }

    /** Barcha qorilar bo'yicha egallangan joy */
    suspend fun totalBytes(): Long = withContext(Dispatchers.IO) {
        root.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    suspend fun delete(reciterId: Int, surahNumber: Int) = withContext(Dispatchers.IO) {
        audioFile(reciterId, surahNumber).delete()
        partFile(reciterId, surahNumber).delete()
        timingsFile(reciterId, surahNumber).delete()
        Unit
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        root.deleteRecursively()
        Unit
    }
}
