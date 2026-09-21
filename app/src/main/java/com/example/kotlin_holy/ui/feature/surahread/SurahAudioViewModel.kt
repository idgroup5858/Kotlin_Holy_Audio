package com.example.kotlin_holy.ui.feature.surahread

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.audio.PlaybackState
import com.example.kotlin_holy.audio.QuranPlayer
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.domain.model.SurahAudio
import com.example.kotlin_holy.domain.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SurahAudioState(
    val surahNumber: Int = 1,
    val download: AudioDownload = AudioDownload.Absent(),
    val playback: PlaybackState = PlaybackState(),
    /** Vaqt jadvali o'qilgan va ijroga tayyor */
    val ready: Boolean = false,
) {
    val downloaded: Boolean get() = download is AudioDownload.Ready
    val downloading: Boolean get() = download is AudioDownload.InProgress

    /** Ijro etilayotgan oyat shu surada bo'lsa uning raqami */
    val playingAyah: Int?
        get() = playback.verseKey
            ?.takeIf { it.substringBefore(':').toIntOrNull() == surahNumber }
            ?.substringAfter(':')
            ?.toIntOrNull()

    /** Ijro etilayotgan so'z — faqat ishonchli vaqt jadvali bo'lganda to'ladi */
    val playingWord: Int? get() = playback.word
}

/**
 * Sura ekranidagi qiroat: yuklash, o'chirish va ijro.
 *
 * O'qish holati (oyatlar, tarjima, xatm) [SurahReadViewModel] da qoladi —
 * audio alohida turgani uchun qiroat yo'q paytda o'qish qismi umuman
 * ta'sirlanmaydi.
 */
@HiltViewModel
class SurahAudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val player: QuranPlayer,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle.get<String>("number")?.toIntOrNull() ?: 1

    /** Yuklangan suraning oyat va so'z vaqtlari */
    private val timings = MutableStateFlow<SurahAudio?>(null)

    val state: StateFlow<SurahAudioState> = combine(
        audioRepository.downloadState(surahNumber),
        player.state,
        timings,
    ) { download, playback, audio ->
        SurahAudioState(
            surahNumber = surahNumber,
            download = download,
            /* Boshqa sura ijro etilayotgan bo'lsa, bu ekranda ko'rsatilmaydi */
            playback = if (playback.surahNumber == surahNumber) playback else PlaybackState(),
            ready = audio != null,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        SurahAudioState(surahNumber = surahNumber),
    )

    init {
        viewModelScope.launch {
            /* Yuklab bo'lingach vaqt jadvali o'qiladi */
            audioRepository.downloadState(surahNumber).collect { download ->
                if (download is AudioDownload.Ready && timings.value == null) {
                    timings.value = audioRepository.audio(surahNumber)
                } else if (download is AudioDownload.Absent) {
                    timings.value = null
                }
            }
        }
    }

    /** Yuklash tugmasida hajmni ko'rsatish uchun; tarmoq bo'lmasa null */
    suspend fun expectedSize(): Long? =
        runCatching { audioRepository.expectedSize(surahNumber) }.getOrNull()

    fun download() {
        viewModelScope.launch { audioRepository.download(surahNumber) }
    }

    fun cancelDownload() {
        audioRepository.cancel(surahNumber)
    }

    fun deleteAudio() {
        viewModelScope.launch {
            player.stop()
            audioRepository.delete(surahNumber)
            timings.value = null
        }
    }

    /** Oyatni boshidan oxirigacha o'qitadi */
    fun playAyah(ayahNumber: Int) {
        val audio = timings.value ?: return
        val file = audioRepository.audioFile(surahNumber) ?: return
        player.playAyah(surahNumber, ayahNumber, file, audio)
    }

    /** Shu oyatdan sura oxirigacha ketma-ket */
    fun playFrom(ayahNumber: Int) {
        val audio = timings.value ?: return
        val file = audioRepository.audioFile(surahNumber) ?: return
        player.playFrom(surahNumber, ayahNumber, file, audio)
    }

    /** So'z ustiga bosilganda — faqat o'sha so'z */
    fun playWord(ayahNumber: Int, wordNumber: Int) {
        val audio = timings.value ?: return
        val file = audioRepository.audioFile(surahNumber) ?: return
        player.playWord(surahNumber, ayahNumber, wordNumber, file, audio)
    }

    fun pause() = player.pause()

    fun resume() = player.resume()

    fun stop() = player.stop()
}
