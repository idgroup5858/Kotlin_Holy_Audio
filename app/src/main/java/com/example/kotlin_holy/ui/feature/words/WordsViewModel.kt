package com.example.kotlin_holy.ui.feature.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.DifficultWord
import com.example.kotlin_holy.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WordsState(
    val loading: Boolean = true,
    val words: List<DifficultWord> = emptyList(),
    val query: String = "",
) {
    val visible: List<DifficultWord>
        get() {
            val q = query.trim().lowercase()
            if (q.isEmpty()) return words
            return words.filter { word ->
                word.text.contains(query.trim()) ||
                    (word.transliteration?.lowercase()?.contains(q) == true)
            }
        }
}

@HiltViewModel
class WordsViewModel @Inject constructor(
    quranRepository: QuranRepository,
) : ViewModel() {

    private val words = MutableStateFlow<List<DifficultWord>>(emptyList())
    private val loading = MutableStateFlow(true)
    private val query = MutableStateFlow("")

    val state: StateFlow<WordsState> = combine(words, loading, query) { list, isLoading, q ->
        WordsState(loading = isLoading, words = list, query = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordsState())

    init {
        viewModelScope.launch {
            words.value = runCatching { quranRepository.words() }.getOrDefault(emptyList())
            loading.value = false
        }
    }

    fun onQueryChange(value: String) {
        query.value = value
    }
}
