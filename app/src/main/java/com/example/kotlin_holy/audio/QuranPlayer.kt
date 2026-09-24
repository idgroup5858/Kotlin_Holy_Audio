package com.example.kotlin_holy.audio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.kotlin_holy.domain.model.SurahAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Hozir nima ijro etilayotgani */
data class PlaybackState(
    val surahNumber: Int? = null,
    val isPlaying: Boolean = false,
    /** Ijro etilayotgan oyat, "2:255" ko'rinishida */
    val verseKey: String? = null,
    /** Shu oyatning nechanchi so'zi o'qilyapti; noma'lum bo'lsa null */
    val word: Int? = null,
    /** Bitta so'z tinglanayotgan bo'lsa true — bunda pleyer paneli ko'rinmaydi */
    val singleWord: Boolean = false,
)

/**
 * Qiroatni boshqaradi.
 *
 * Ijro [PlaybackService] ichida ketadi, bu sinf esa unga ulanib buyruq beradi.
 *
 * Ishlash tezligi haqida: o'qilayotgan so'zni topish uchun pleyer vaqti qisqa
 * oraliqda o'lchab turiladi, lekin holat faqat **so'z almashganda** yangilanadi.
 * Shu sababli ekran soniyasiga o'nlab marta emas, so'z sayin bir marta qayta
 * chiziladi — varaqlash va aylantirish ijro paytida ham silliq qoladi.
 */
@Singleton
class QuranPlayer @Inject constructor(
    private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controller: MediaController? = null
    private var tick: Job? = null

    /** Joriy suraning vaqt jadvali — so'zni aniqlash uchun kerak */
    private var audio: SurahAudio? = null

    /**
     * MediaItem kesib olinganda pleyer vaqtni kesma boshidan sanaydi, shuning
     * uchun fayl boshidan hisoblangan vaqtni olish uchun shu qiymat qo'shiladi.
     */
    private var clipStartMs: Long = 0

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    /* ---------- ulanish ---------- */

    private suspend fun controller(): MediaController {
        controller?.takeIf { it.isConnected }?.let { return it }
        val connected = connect()
        connected.addListener(listener)
        controller = connected
        return connected
    }

    private suspend fun connect(): MediaController = suspendCancellableCoroutine { continuation ->
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { continuation.resume(it) }
                    .onFailure { continuation.resumeWithException(it) }
            },
            ContextCompat.getMainExecutor(context),
        )
        continuation.invokeOnCancellation { future.cancel(true) }
    }

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) startTicking() else stopTicking()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                stopTicking()
                _state.value = _state.value.copy(isPlaying = false, word = null)
            }
        }
    }

    /* ---------- ijro ---------- */

    /** Suraning bir oyatini boshidan oxirigacha o'qiydi */
    fun playAyah(surahNumber: Int, ayahNumber: Int, file: File, surahAudio: SurahAudio) {
        val timing = surahAudio.timing(ayahNumber) ?: return
        start(
            surahNumber = surahNumber,
            file = file,
            surahAudio = surahAudio,
            fromMs = timing.startMs,
            toMs = timing.endMs,
            verseKey = timing.verseKey,
            singleWord = false,
        )
    }

    /** Shu oyatdan boshlab sura oxirigacha ketma-ket o'qiydi */
    fun playFrom(surahNumber: Int, ayahNumber: Int, file: File, surahAudio: SurahAudio) {
        val timing = surahAudio.timing(ayahNumber) ?: return
        start(
            surahNumber = surahNumber,
            file = file,
            surahAudio = surahAudio,
            fromMs = timing.startMs,
            toMs = null,
            verseKey = timing.verseKey,
            singleWord = false,
        )
    }

    /** Bitta so'zni o'qiydi — so'z ustiga bosilganda */
    fun playWord(
        surahNumber: Int,
        ayahNumber: Int,
        wordNumber: Int,
        file: File,
        surahAudio: SurahAudio,
    ) {
        val timing = surahAudio.timing(ayahNumber) ?: return
        val segment = timing.words.firstOrNull { it.position == wordNumber } ?: return
        start(
            surahNumber = surahNumber,
            file = file,
            surahAudio = surahAudio,
            fromMs = segment.startMs,
            toMs = segment.endMs,
            verseKey = timing.verseKey,
            singleWord = true,
            word = wordNumber,
        )
    }

    private fun start(
        surahNumber: Int,
        file: File,
        surahAudio: SurahAudio,
        fromMs: Long,
        toMs: Long?,
        verseKey: String,
        singleWord: Boolean,
        word: Int? = null,
    ) {
        audio = surahAudio
        clipStartMs = fromMs

        val clipping = MediaItem.ClippingConfiguration.Builder()
            .setStartPositionMs(fromMs)
            .apply { if (toMs != null) setEndPositionMs(toMs) }
            .build()

        val item = MediaItem.Builder()
            .setUri(file.toUri())
            .setClippingConfiguration(clipping)
            .build()

        _state.value = PlaybackState(
            surahNumber = surahNumber,
            isPlaying = true,
            verseKey = verseKey,
            word = word,
            singleWord = singleWord,
        )

        scope.launch {
            val player = runCatching { controller() }.getOrNull() ?: return@launch
            player.setMediaItem(item)
            player.prepare()
            player.play()
        }
    }

    fun pause() {
        scope.launch { runCatching { controller() }.getOrNull()?.pause() }
    }

    fun resume() {
        scope.launch { runCatching { controller() }.getOrNull()?.play() }
    }

    fun stop() {
        stopTicking()
        _state.value = PlaybackState()
        scope.launch {
            runCatching { controller() }.getOrNull()?.let { player ->
                player.stop()
                player.clearMediaItems()
            }
        }
    }

    /* ---------- so'zni kuzatish ---------- */

    private fun startTicking() {
        if (tick?.isActive == true) return
        tick = scope.launch {
            while (true) {
                val player = PlaybackService.activePlayer ?: controller ?: break
                val absolute = clipStartMs + player.currentPosition
                val current = audio?.ayahAt(absolute)

                /*
                 * Yangi qiymat eskisi bilan bir xil bo'lsa, StateFlow uni
                 * tarqatmaydi — demak ekran qayta chizilmaydi. Shuning uchun
                 * bu tsikl tez-tez ishlasa ham UI ga yuk tushmaydi.
                 */
                val next = _state.value.copy(
                    verseKey = current?.verseKey ?: _state.value.verseKey,
                    word = current?.wordAt(absolute),
                )
                if (next != _state.value) _state.value = next

                delay(TICK_MS)
            }
        }
    }

    private fun stopTicking() {
        tick?.cancel()
        tick = null
    }

    private companion object {
        /** So'z chegarasini sezilarli kechikishsiz ilg'aydigan eng katta oraliq */
        const val TICK_MS = 60L
    }
}

private fun File.toUri() = android.net.Uri.fromFile(this)
