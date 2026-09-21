package com.example.kotlin_holy.ui.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.audio.PlaybackState
import com.example.kotlin_holy.audio.QuranPlayer
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.domain.model.SurahAudio
import com.example.kotlin_holy.domain.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderAudioState(
    val downloads: Map<Int, AudioDownload> = emptyMap(),
    val playback: PlaybackState = PlaybackState(),
) {
    fun downloadOf(surahNumber: Int): AudioDownload =
        downloads[surahNumber] ?: AudioDownload.Absent()

    fun isReady(surahNumber: Int): Boolean = downloads[surahNumber] is AudioDownload.Ready

    /** Ijro etilayotgan oyatning "sura:oyat" kaliti */
    val playingVerse: String? get() = playback.verseKey

    val playingWord: Int? get() = playback.word

    val isPlayingAyah: Boolean get() = playback.isPlaying && !playback.singleWord
}

/**
 * Sahifa va juz ekranlaridagi qiroat.
 *
 * Sura ekranidan farqi shunda: bir bet ikki suradan iborat bo'lishi mumkin
 * (masalan 293-bet), juz esa o'nlab surani qamraydi. Audio esa sura-ba-sura
 * yuklanadi. Shuning uchun bu model bitta suraga bog'lanmaydi — betdagi har bir
 * sura uchun alohida holat beradi va qaysi sura bosilsa o'shani ijro etadi.
 *
 * Vaqt jadvali oldindan emas, kerak bo'lganda o'qiladi: aks holda yuklangan
 * suralar ko'paygan sayin ekran ochilishi sekinlashardi.
 */
@HiltViewModel
class ReaderAudioViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val player: QuranPlayer,
) : ViewModel() {

    private val timings = HashMap<Int, SurahAudio>()

    val state: StateFlow<ReaderAudioState> = combine(
        audioRepository.downloads,
        player.state,
    ) { downloads, playback ->
        ReaderAudioState(downloads = downloads, playback = playback)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderAudioState())

    init {
        /* Sura o'chirilsa yoki qori almashsa, eski jadval yaroqsiz bo'ladi */
        viewModelScope.launch {
            audioRepository.downloads.collect { downloads ->
                val stale = timings.keys.filter { downloads[it] !is AudioDownload.Ready }
                stale.forEach { timings.remove(it) }
            }
        }
    }

    private suspend fun timingsFor(surahNumber: Int): SurahAudio? =
        timings[surahNumber] ?: audioRepository.audio(surahNumber)?.also {
            timings[surahNumber] = it
        }

    fun download(surahNumber: Int) {
        viewModelScope.launch { audioRepository.download(surahNumber) }
    }

    fun cancelDownload(surahNumber: Int) {
        audioRepository.cancel(surahNumber)
    }

    fun deleteAudio(surahNumber: Int) {
        viewModelScope.launch {
            player.stop()
            audioRepository.delete(surahNumber)
            timings.remove(surahNumber)
        }
    }

    suspend fun expectedSize(surahNumber: Int): Long? =
        runCatching { audioRepository.expectedSize(surahNumber) }.getOrNull()

    fun playAyah(surahNumber: Int, ayahNumber: Int) = viewModelScope.launch {
        val audio = timingsFor(surahNumber) ?: return@launch
        val file = audioRepository.audioFile(surahNumber) ?: return@launch
        player.playAyah(surahNumber, ayahNumber, file, audio)
    }

    fun playFrom(surahNumber: Int, ayahNumber: Int) = viewModelScope.launch {
        val audio = timingsFor(surahNumber) ?: return@launch
        val file = audioRepository.audioFile(surahNumber) ?: return@launch
        player.playFrom(surahNumber, ayahNumber, file, audio)
    }

    fun playWord(surahNumber: Int, ayahNumber: Int, wordNumber: Int) = viewModelScope.launch {
        val audio = timingsFor(surahNumber) ?: return@launch
        val file = audioRepository.audioFile(surahNumber) ?: return@launch
        player.playWord(surahNumber, ayahNumber, wordNumber, file, audio)
    }

    fun pause() = player.pause()

    fun stop() = player.stop()
}
