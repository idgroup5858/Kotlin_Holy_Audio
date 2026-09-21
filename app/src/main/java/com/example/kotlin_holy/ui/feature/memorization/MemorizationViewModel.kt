package com.example.kotlin_holy.ui.feature.memorization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.MEMORIZATION_TARGET
import com.example.kotlin_holy.domain.repository.MemorizationRepository
import com.example.kotlin_holy.memorization.RecitationEvent
import com.example.kotlin_holy.memorization.RecitationRecognizer
import com.example.kotlin_holy.memorization.matchRecitation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemorizationUiState(
    /** Hozir kengaytirilgan (yodlash paneli ochiq) oyat */
    val activeVerseKey: String? = null,
    val listening: Boolean = false,
    val streak: Int = 0,
    val memorized: Boolean = false,
    /** Oxirgi urinish natijasi: true — to'g'ri, false — xato, null — hali urinish bo'lmagan */
    val lastResult: Boolean? = null,
    val error: String? = null,
) {
    val target: Int get() = MEMORIZATION_TARGET
}

/**
 * Oyatni ovoz orqali yodlash: mikrofon orqali tinglaydi, o'qilgan matnni
 * asl oyat bilan solishtiradi va ketma-ket [MEMORIZATION_TARGET] marta to'g'ri
 * o'qilganda oyatni "yodlandi" deb belgilaydi.
 *
 * Sura, sahifa va juz ekranlari o'zining "Oyat bo'yicha" ro'yxatida bu
 * ViewModel'ni alohida-alohida (hiltViewModel() chaqirilganda) yaratadi.
 */
@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val recognizer: RecitationRecognizer,
    private val repository: MemorizationRepository,
) : ViewModel() {

    private val _ui = MutableStateFlow(MemorizationUiState())
    val ui: StateFlow<MemorizationUiState> = _ui

    val progress = repository.progress.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyMap(),
    )

    private var listenJob: Job? = null

    /** Oyat uchun yodlash panelini ochadi */
    fun open(verseKey: String) {
        if (_ui.value.activeVerseKey == verseKey) return
        listenJob?.cancel()
        val current = progress.value[verseKey]
        _ui.value = MemorizationUiState(
            activeVerseKey = verseKey,
            streak = current?.correctStreak ?: 0,
            memorized = current?.isMemorized ?: false,
        )
    }

    fun close() {
        listenJob?.cancel()
        _ui.value = MemorizationUiState()
    }

    fun startListening(expectedArabic: String) {
        val verseKey = _ui.value.activeVerseKey ?: return
        if (_ui.value.listening) return
        listenJob?.cancel()
        listenJob = viewModelScope.launch {
            recognizer.listen().collect { event ->
                when (event) {
                    is RecitationEvent.Listening ->
                        _ui.update { it.copy(listening = true, error = null, lastResult = null) }

                    is RecitationEvent.FinalResult -> {
                        val match = matchRecitation(expectedArabic, event.text)
                        repository.recordAttempt(verseKey, match.correct)
                        val updated = repository.progress.first()[verseKey]
                        _ui.update {
                            it.copy(
                                listening = false,
                                lastResult = match.correct,
                                streak = updated?.correctStreak ?: 0,
                                memorized = updated?.isMemorized ?: false,
                            )
                        }
                    }

                    is RecitationEvent.Error ->
                        _ui.update { it.copy(listening = false, error = event.message) }
                }
            }
        }
    }

    fun stopListening() {
        listenJob?.cancel()
        _ui.update { it.copy(listening = false) }
    }

    fun resetProgress() {
        val verseKey = _ui.value.activeVerseKey ?: return
        viewModelScope.launch {
            repository.reset(verseKey)
            _ui.update { it.copy(streak = 0, memorized = false, lastResult = null) }
        }
    }
}
