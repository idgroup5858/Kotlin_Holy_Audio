package com.example.kotlin_holy.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.repository.AppSettings
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.ReadingTimeRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.shell.ReadingTracker
import com.example.kotlin_holy.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Xatm holati — navbardagi ko'rsatkich uchun */
data class XatmSummary(
    val pages: Map<Int, Long> = emptyMap(),
    val startedAt: Long? = null,
)

/** Mavzu, sozlamalar, sura ro'yxati va navbar ko'rsatkichlarini butun ilovaga tarqatadi */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    quranRepository: QuranRepository,
    xatmRepository: XatmRepository,
    readingTimeRepository: ReadingTimeRepository,
    val readingTracker: ReadingTracker,
) : ViewModel() {

    val settings: StateFlow<AppSettings?> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs

    val xatm: StateFlow<XatmSummary> = combine(xatmRepository.readPages, xatmRepository.startedAt) { pages, started ->
        XatmSummary(pages, started)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XatmSummary())

    /** Bugungi o'qish vaqti: saqlangan + hali yozilmagan */
    val todayReading: StateFlow<Long> = combine(readingTimeRepository.totals, readingTracker.unsaved) { saved, unsaved ->
        val today = ReadingTracker.dayKey(System.currentTimeMillis())
        (saved[today] ?: 0L) + (unsaved[today] ?: 0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    init {
        viewModelScope.launch {
            _surahs.value = runCatching { quranRepository.surahs() }.getOrDefault(emptyList())
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
