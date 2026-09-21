package com.example.kotlin_holy.data.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/**
 * Qiroat internetdan olinadi: avval oyat va so'z vaqtlari jadvali, so'ng
 * suraning yaxlit mp3 fayli.
 *
 * Yuklash uzilib qolsa, qaytadan boshlanmaydi — server Range so'rovlarini
 * qo'llab-quvvatlaydi (tekshirilgan), shuning uchun yarim yuklangan ".part"
 * faylning davomi so'raladi.
 */
@Singleton
class AudioRemoteSource @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /** Sura audiosi haqidagi ma'lumot: havola va vaqt jadvali */
    suspend fun chapterAudio(reciterId: Int, surahNumber: Int): ChapterAudioFileDto? =
        withContext(Dispatchers.IO) {
            val url = "$API_BASE/chapter_recitations/$reciterId/$surahNumber?segments=true"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                runCatching {
                    json.decodeFromString<ChapterAudioResponseDto>(body).audioFile
                }.getOrNull()
            }
        }

    /**
     * Faylning haqiqiy hajmi. API ba'zi qorilar uchun 0 qaytargani sababli
     * hajm sarlavhadan so'raladi.
     */
    suspend fun sizeOf(audioUrl: String): Long? = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(audioUrl).head().build()
        runCatching {
            client.newCall(request).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull()
            }
        }.getOrNull()
    }

    /**
     * Mp3 ni [target] ga yuklaydi. Fayl allaqachon qisman yuklangan bo'lsa,
     * qolgan qismi so'raladi. [onProgress] har bo'lakda chaqiriladi.
     */
    suspend fun download(
        audioUrl: String,
        target: File,
        onProgress: suspend (downloaded: Long, total: Long?) -> Unit,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val already = if (target.isFile) target.length() else 0L
            val builder = Request.Builder().url(audioUrl)
            if (already > 0) builder.header("Range", "bytes=$already-")

            client.newCall(builder.build()).execute().use { response ->
                /* 206 — davomi berildi, 200 — server boshidan qaytardi */
                val resuming = response.code == 206
                if (!response.isSuccessful) error("Server javobi: ${response.code}")
                val body = response.body ?: error("Javob bo'sh")

                val startAt = if (resuming) already else 0L
                val total = body.contentLength().takeIf { it > 0 }?.let { it + startAt }

                target.parentFile?.mkdirs()
                var written = startAt
                body.byteStream().use { input ->
                    java.io.FileOutputStream(target, resuming).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            written += read
                            onProgress(written, total)
                        }
                        output.flush()
                    }
                }
            }
        }
    }

    private companion object {
        const val API_BASE = "https://api.quran.com/api/v4"
        const val DOWNLOAD_BUFFER = 64 * 1024
    }
}
