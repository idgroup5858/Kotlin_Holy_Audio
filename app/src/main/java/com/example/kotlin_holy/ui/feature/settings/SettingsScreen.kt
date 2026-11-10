package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import com.example.kotlin_holy.domain.repository.AppSettings
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.ui.components.HolyTabs
import com.example.kotlin_holy.ui.components.PageHeader
import com.example.kotlin_holy.ui.navigation.BackLink
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.NavActions
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.PageWidth
import com.example.kotlin_holy.ui.theme.Appearance
import com.example.kotlin_holy.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sozlamalar tabi: nomi va sahifa sarlavhasi ostidagi izoh */
private data class SettingsTab(val id: String, val label: String, val hint: String)

/*
 * Veb nusxadagi SettingsPage.jsx dagi TABS. «sayt / brauzer» so'zlari ilovaga moslangan;
 * «Qiroat» — faqat Android nusxasida (qori tanlash va yuklangan audio).
 */
private val TABS = listOf(
    SettingsTab("text", "Matn oʻlchami", "Sura, juz va sahifalarni oʻqishdagi matn oʻlchamlari."),
    SettingsTab("theme", "Mavzu", "Butun ilova koʻrinishi: ranglar, orqa fon, kartochkalar va burchaklar."),
    SettingsTab("data", "Maʼlumot manbai", "Ilova assets/data ichidagi statik JSON fayllardan oʻqiydi."),
    SettingsTab("xatm", "Xatm", "Oʻqilgan sahifalar belgilari — shu qurilmada saqlanadi."),
    SettingsTab(
        "reading",
        "Oʻqish vaqti",
        "Sura, sahifa va juz oʻqish sahifalarida oʻtkazilgan vaqt — kunlar boʻyicha.",
    ),
    SettingsTab(
        "reminders",
        "Eslatmalar",
        "Bosh sahifadagi «Bugungi eslatma» uchun oʻzingiz tanlagan oyat va hikmatlar.",
    ),
    SettingsTab("audio", "Qiroat", "Qori tanlash va yuklangan audio — faqat Android ilovasida."),
)

private fun validTab(id: String?): String? = id?.takeIf { value -> TABS.any { it.id == value } }

/**
 * Sozlamalar sahifasi: tablar holati va mavzu / matn o'lchami sozlamalari.
 * Qolgan tablarning o'z ViewModel'i bor (Xatm, O'qish vaqti, Eslatmalar …).
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    /* ?tab=reminders kabi havola bilan kerakli tab ochiladi; tanlangani esa eslab qolinadi */
    private val _tab = MutableStateFlow(
        validTab(savedStateHandle[SELECTED_KEY]) ?: validTab(savedStateHandle["tab"]) ?: TABS.first().id,
    )
    val tab: StateFlow<String> = _tab.asStateFlow()

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun selectTab(id: String) {
        val tab = validTab(id) ?: return
        _tab.value = tab
        savedStateHandle[SELECTED_KEY] = tab
    }

    /* Navigatsiya yozuvi — bir xil yozuv qayta kelsa (aylantirish, orqaga qaytish) tab o'zgarmaydi */
    private var seenEntry: Any? = null
    private var seenAny = false

    /**
     * Sozlamalar ochiq turganda navbardagi havola (xatm, o'qish vaqti) bosilsa, yangi
     * yozuv shu sahifaning o'zida ochiladi — so'ralgan tab ko'rsatiladi.
     */
    fun onNavEntry(entry: Any?, requestedTab: String?) {
        if (!seenAny) {
            seenAny = true
            seenEntry = entry
            return
        }
        if (entry === seenEntry) return
        seenEntry = entry
        validTab(requestedTab)?.let(::selectTab)
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setAppearance(appearance: Appearance) {
        viewModelScope.launch { settingsRepository.setAppearance(appearance) }
    }

    fun setArabicScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setArabicScale(scale) }
    }

    fun setTranslationScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setTranslationScale(scale) }
    }

    private companion object {
        const val SELECTED_KEY = "settings_selected_tab"
    }
}

/**
 * Veb nusxadagi SettingsPage.jsx: «‹ Orqaga» (kelgan sahifaga), sarlavha va faol tabning
 * izohi, tablar va tab mazmuni.
 */
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val tab by viewModel.tab.collectAsStateWithLifecycle()

    /* Sozlamalarda turganda qaytish nuqtasi o'zgarmaydi — bir marta o'qiladi */
    val nav = LocalNav.current
    val backLabel = remember { nav.previousLabel() }
    BackLink(NavActions.UP, backLabel)

    val entry = LocalViewModelStoreOwner.current as? NavBackStackEntry
    LaunchedEffect(entry) { viewModel.onNavEntry(entry, entry?.arguments?.getString("tab")) }

    val active = TABS.firstOrNull { it.id == tab } ?: TABS.first()
    /* Eslatmalar ro'yxati har biri alohida qator bo'lib chiziladi — holati shu yerda olinadi */
    val reminders = if (active.id == "reminders") rememberReminderTabState() else null

    HolyPage(maxWidth = PageWidth.Section) {
        item(key = "settings-header") {
            PageHeader(title = "Sozlamalar", subtitle = active.hint)
        }
        item(key = "settings-tabs") {
            HolyTabs(
                tabs = TABS.map { it.id to it.label },
                value = active.id,
                onChange = viewModel::selectTab,
                modifier = Modifier.padding(bottom = 24.dp),
            )
        }
        when (active.id) {
            "text" -> textSettings()
            "theme" -> themeSettings()
            "data" -> dataSourceSettings()
            "xatm" -> xatmSettings()
            "reading" -> readingTimeSettings()
            "reminders" -> if (reminders != null) reminderSettings(reminders)
            "audio" -> audioSettings()
        }
    }
}
