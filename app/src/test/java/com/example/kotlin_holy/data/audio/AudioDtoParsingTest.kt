package com.example.kotlin_holy.data.audio

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Qiroat API'si vaqtlarni ko'p qorilarda kasr son bilan qaytaradi
 * ("2300.0", "16219.999999999998"). Avval ular Long deb o'qilgani uchun
 * Kahf va boshqa suralar yuklanmay qolardi — bu test shuni qaytarmaydi.
 */
class AudioDtoParsingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    @Test
    fun decimalTimestampsAreAccepted() {
        /* Husariy (6), Kahf 18:2 dan qisqartirilgan haqiqiy javob */
        val body = """
            {"audio_file":{"id":1,"chapter_id":18,"file_size":45920384.0,
             "audio_url":"https://download.quranicaudio.com/qdc/khalil_al_husary/murattal/18.mp3",
             "timestamps":[{"verse_key":"18:2","timestamp_from":16220,"timestamp_to":31970.0,
               "duration":15750,"segments":[[1,16219.999999999998,17230.0],[2,17230.0,19340.0],[3,19340]]}]}}
        """.trimIndent()

        val file = json.decodeFromString<ChapterAudioResponseDto>(body).audioFile!!
        val stamp = file.timestamps.single()

        assertEquals(16220L, stamp.from.toMillis())
        assertEquals(31970L, stamp.to.toMillis())
        assertEquals(listOf(1L, 16220L, 17230L), stamp.segments[0].map { it.toMillis() })
        assertEquals(2, stamp.segments[2].size)
    }

    @Test
    fun nullFileSizeFallsBackToZero() {
        val body = """{"audio_file":{"file_size":null,"audio_url":"x","timestamps":[]}}"""
        assertEquals(0.0, json.decodeFromString<ChapterAudioResponseDto>(body).audioFile!!.fileSize, 0.0)
    }
}
