package com.example.kotlin_holy.memorization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface RecitationEvent {
    data object Listening : RecitationEvent
    data class FinalResult(val text: String) : RecitationEvent
    data class Error(val message: String) : RecitationEvent
}

/**
 * Android'ning tizim nutqni tanish xizmati (arabcha, `ar` locale) ustidan
 * yupqa qatlam. Har [listen] chaqirilganda yangi tinglash seansi boshlanadi;
 * bitta vaqtda faqat bitta seans faol bo'ladi.
 *
 * [SpeechRecognizer] chaqiruvlari asosiy oqimda bo'lishi shart, shuning uchun
 * bu funksiya ViewModel ichida `viewModelScope`dan (standart Main.immediate)
 * to'g'ridan-to'g'ri to'planishi kerak — boshqa dispatcher'ga o'tkazilmasin.
 */
@Singleton
class RecitationRecognizer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun listen(): Flow<RecitationEvent> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(RecitationEvent.Error("Bu qurilmada nutqni tanish mavjud emas"))
            close()
            return@callbackFlow
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                trySend(RecitationEvent.Listening)
            }

            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit

            override fun onError(error: Int) {
                trySend(RecitationEvent.Error(errorMessage(error)))
                close()
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                trySend(RecitationEvent.FinalResult(text))
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Ovoz tanilmadi, qayta urinib ko'ring"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Gapirish eshitilmadi"
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Internet aloqasi yo'q — nutqni tanish uchun internet kerak"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Mikrofon uchun ruxsat berilmagan"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Nutqni tanish band, birozdan keyin urinib ko'ring"
        SpeechRecognizer.ERROR_AUDIO -> "Mikrofonda xatolik yuz berdi"
        else -> "Xatolik yuz berdi, qayta urinib ko'ring"
    }
}
