package com.example.kotlin_holy.ui.feature.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.PageContent
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AyahExtras
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.PageTranslation
import com.example.kotlin_holy.ui.components.PagerAction
import com.example.kotlin_holy.ui.components.ReadMarkButton
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.Segmented
import com.example.kotlin_holy.ui.components.SurahInfo
import com.example.kotlin_holy.ui.feature.home.AyahSaver
import com.example.kotlin_holy.ui.navigation.BackLink
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.PageWidth
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ReadingText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

val TEXT_MODES = listOf("both" to "Ikkalasi", "arabic" to "Arabcha", "translation" to "Tarjima")
val VIEW_MODES = listOf("ayah" to "Oyat boʻyicha", "page" to "Sahifa boʻyicha")

/**
 * Suralar bo'limidan ochilgan bet (surah/S/page/N) shu sura ichida qoladi. Oldingi yoki
 * keyingi bet sura chegarasidan chiqsa, o'sha betni egallagan qo'shni suraga o'tiladi.
 */
private fun surahForPage(surahNumber: Int, page: Int): Int {
    val start = JuzCatalog.surahPageRange(surahNumber) ?: return surahNumber
    val step = if (page < start.first) -1 else 1
    var number = surahNumber
    while (number in 1..114) {
        val range = JuzCatalog.surahPageRange(number) ?: break
        if (page in range) return number
        number += step
    }
    return surahNumber
}

/** Juzlar bo'limidan ochilgan bet ham shunday: chegaradagi bet joriy juz ichida qoladi */
private fun juzForPage(juzNumber: Int, page: Int): Int {
    val start = JuzCatalog.juzPageRange(juzNumber) ?: return juzNumber
    val step = if (page < start.first) -1 else 1
    var number = juzNumber
    while (number in 1..30) {
        val range = JuzCatalog.juzPageRange(number) ?: break
        if (page in range) return number
        number += step
    }
    return juzNumber
}

data class PageReadState(
    val number: Int,
    val contextSurah: Int?,
    val contextJuz: Int?,
    val loading: Boolean = true,
    val content: PageContent? = null,
    val mushaf: MushafPage? = null,
    val surahs: List<Surah> = emptyList(),
    val readAt: Long? = null,
    val xatm: Map<Int, Long> = emptyMap(),
    val textMode: String = "both",
    val tafsir: Map<String, String> = emptyMap(),
    val saved: Set<String> = emptySet(),
)

@HiltViewModel
class PageReadViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val xatmRepository: XatmRepository,
    private val settingsRepository: SettingsRepository,
    private val ayahSaver: AyahSaver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val number: Int = savedStateHandle.get<String>("number")?.toIntOrNull()?.coerceIn(1, 604) ?: 1

    /* Yo'ldagi sura yoki juz raqami — bet uning ichida bo'lsagina */
    val contextSurah: Int? = savedStateHandle.get<String>("surah")?.toIntOrNull()
        ?.takeIf { JuzCatalog.surahPageRange(it)?.contains(number) == true }
    val contextJuz: Int? = savedStateHandle.get<String>("juz")?.toIntOrNull()
        ?.takeIf { JuzCatalog.juzPageRange(it)?.contains(number) == true }

    fun pathOf(page: Int): String = when {
        contextSurah != null -> Routes.surahPage(surahForPage(contextSurah, page), page)
        contextJuz != null -> Routes.juzPage(juzForPage(contextJuz, page), page)
        else -> Routes.page(page)
    }

    private val loaded = MutableStateFlow(PageReadState(number, contextSurah, contextJuz))

    val state: StateFlow<PageReadState> = combine(
        loaded,
        xatmRepository.readPages,
        settingsRepository.settings,
        ayahSaver.savedVerses,
    ) { base, xatm, settings, saved ->
        base.copy(readAt = xatm[number], xatm = xatm, textMode = settings.textMode, saved = saved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PageReadState(number, contextSurah, contextJuz))

    init {
        viewModelScope.launch {
            val content = runCatching { quranRepository.page(number) }.getOrNull()
            val mushaf = runCatching { quranRepository.mushafPage(number) }.getOrNull()
            val surahs = runCatching { quranRepository.surahs() }.getOrDefault(emptyList())
            loaded.value = loaded.value.copy(loading = false, content = content, mushaf = mushaf, surahs = surahs)
            if (content != null) settingsRepository.setLastRead(pathOf(number), "$number-sahifa")
            /* Betdagi suralar tafsiri */
            val tafsir = HashMap<String, String>()
            content?.ayahs?.map { it.surahNumber }?.distinct()?.forEach { surah ->
                runCatching { quranRepository.tafsir(surah) }.getOrNull()?.forEach { (ayah, text) -> tafsir["$surah:$ayah"] = text }
            }
            loaded.value = loaded.value.copy(tafsir = tafsir)
        }
    }

    fun toggleRead() {
        viewModelScope.launch { xatmRepository.toggle(number) }
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }

    fun toggleSave(ayah: com.example.kotlin_holy.domain.model.Ayah) {
        viewModelScope.launch { ayahSaver.toggle(ayah) }
    }
}

/** page/N, surah/S/page/N, juz/J/page/N — mushaf beti va tarjimasi */
@Composable
fun PageReadScreen(
    viewModel: PageReadViewModel = hiltViewModel(),
    audioViewModel: ReaderAudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val nav = LocalNav.current
    val current = state.number
    val content = state.content
    val surahName = state.surahs.firstOrNull { it.number == state.contextSurah }?.nameUz

    BackLink(
        route = when {
            state.contextSurah != null -> Routes.surahPages(state.contextSurah!!)
            state.contextJuz != null -> Routes.juzPages(state.contextJuz!!)
            else -> Routes.PAGES
        },
        label = when {
            state.contextSurah != null -> if (surahName != null) "$surahName sahifalari" else "Sura sahifalari"
            state.contextJuz != null -> "${state.contextJuz}-juz sahifalari"
            else -> "Sahifalar"
        },
    )

    val showArabic = state.textMode != "translation"
    val showTranslation = state.textMode != "arabic"
    val juz = content?.juz ?: content?.ayahs?.firstOrNull()?.juz
    val surahsOnPage = content?.ayahs.orEmpty().map { it.surahNumber }.distinct()
        .mapNotNull { number -> state.surahs.firstOrNull { it.number == number } }
    /* Shu betda boshlanadigan suralar (1-oyati shu betda) — ular haqida ma'lumot ko'rsatiladi */
    val startingSurahs = content?.ayahs.orEmpty().filter { it.numberInSurah == 1 }
        .mapNotNull { ayah -> state.surahs.firstOrNull { it.number == ayah.surahNumber } }
    val extras = AyahExtras(state.tafsir, state.saved, viewModel::toggleSave)

    HolyPage(maxWidth = PageWidth.Reading) {
        item(key = "title") {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                Icon(HolyIcons.Bookmark, null, tint = colors.inkFaint, modifier = Modifier.size(16.dp))
                Text("$current-sahifa", color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                if (juz != null) Text("Juz $juz", color = colors.inkFaint, fontSize = 14.sp)
            }
        }
        item(key = "modes") {
            Row(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalArrangement = Arrangement.End) {
                Segmented(TEXT_MODES, state.textMode, viewModel::setTextMode)
            }
        }

        if (state.loading) item(key = "loading") { LoadingRows(3) }

        if (!state.loading && content == null) {
            item(key = "missing") {
                EmptyState(title = "$current-sahifa boʻyicha maʼlumot yoʻq", hint = "Bu sahifa maʼlumot fayllarida topilmadi.")
            }
        }

        if (content != null) {
            if (surahsOnPage.isNotEmpty()) {
                item(key = "surah-badges") {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        surahsOnPage.forEach { surah ->
                            HolyBadge("${surah.number}. ${surah.nameUz}", tone = BadgeTone.Accent, onClick = { nav.open(Routes.surah(surah.number)) })
                        }
                    }
                }
            }
            if (startingSurahs.isNotEmpty()) {
                item(key = "starting") {
                    HolyCard(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                        Column {
                            startingSurahs.forEachIndexed { index, surah ->
                                if (index > 0) Divider()
                                SurahInfo(surah, state.xatm, Modifier.padding(20.dp))
                            }
                        }
                    }
                }
            }
            item(key = "audio") {
                SurahAudioCards(surahsOf(content.ayahs), audio, audioViewModel, Modifier.padding(bottom = 20.dp))
            }
            item(key = "page") {
                HolyCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp)) {
                        if (showArabic) {
                            if (state.mushaf != null || content.ayahs.isNotEmpty()) {
                                AudioMushafPage(state.mushaf, audio, audioViewModel)
                            } else {
                                Text(
                                    "Bu sahifaning arabcha matni kiritilmagan",
                                    color = colors.inkFaint,
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (showArabic && showTranslation) Divider(Modifier.padding(vertical = 32.dp))
                        if (showTranslation) {
                            if (content.ayahs.isNotEmpty()) {
                                PageTranslation(content.ayahs, extras)
                            } else {
                                Text(
                                    "Bu sahifaning tarjimasi kiritilmagan",
                                    color = colors.inkFaint,
                                    fontSize = 14.sp,
                                    fontStyle = FontStyle.Italic,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                        if (!content.note.isNullOrBlank()) {
                            Divider(Modifier.padding(top = 32.dp))
                            Text(content.note, style = ReadingText.uzSub, modifier = Modifier.padding(top = 20.dp))
                        }
                    }
                }
            }
            item(key = "mark") { ReadMarkButton(state.readAt, viewModel::toggleRead) }
        }

        item(key = "pager") {
            ReaderPager(
                prev = if (current > 1) PagerAction("Oldingi sahifa") { nav.replace(viewModel.pathOf(current - 1)) } else null,
                next = if (current < 604) PagerAction("Keyingi sahifa") { nav.replace(viewModel.pathOf(current + 1)) } else null,
                label = "$current / 604",
            )
        }
    }
}
