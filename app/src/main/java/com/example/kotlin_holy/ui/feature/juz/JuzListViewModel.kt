package com.example.kotlin_holy.ui.feature.juz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.repository.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Juz chegaralari doimiy ro'yxatdan olinadi, faqat sura nomlari kerak */
@HiltViewModel
class JuzListViewModel @Inject constructor(
    quranRepository: QuranRepository,
) : ViewModel() {

    private val _surahNames = MutableStateFlow<Map<Int, String>>(emptyMap())
    val surahNames: StateFlow<Map<Int, String>> = _surahNames.asStateFlow()

    init {
        viewModelScope.launch {
            _surahNames.value = runCatching {
                quranRepository.surahs().associate { it.number to it.nameUz }
            }.getOrDefault(emptyMap())
        }
    }
}
