package com.example.kotlin_holy.ui.feature.surah

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.SurahDetail
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.AyahExtras
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.PageTilesSkeleton
import com.example.kotlin_holy.ui.components.PagerAction
import com.example.kotlin_holy.ui.components.ReadMarkButton
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.Segmented
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.components.SurahInfo
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.dayColors
import com.example.kotlin_holy.ui.components.pageGridColumns
import com.example.kotlin_holy.ui.components.surahsByPage
import com.example.kotlin_holy.ui.feature.home.AyahSaver
import com.example.kotlin_holy.ui.feature.home.QuranListsViewModel
import com.example.kotlin_holy.ui.feature.pages.AYAHS_PER_VIEW
import com.example.kotlin_holy.ui.feature.pages.PageGroup
import com.example.kotlin_holy.ui.feature.pages.PageGroupCard
import com.example.kotlin_holy.ui.feature.pages.SurahAudioCards
import com.example.kotlin_holy.ui.feature.pages.TEXT_MODES
import com.example.kotlin_holy.ui.feature.pages.VIEW_MODES
import com.example.kotlin_holy.ui.feature.pages.ayahAudio
import com.example.kotlin_holy.ui.feature.pages.ayahRows
import com.example.kotlin_holy.ui.feature.pages.contentWidth
import com.example.kotlin_holy.ui.feature.pages.groupByPage
import com.example.kotlin_holy.ui.feature.pages.pageTiles
import com.example.kotlin_holy.ui.feature.pages.surahsOf
import com.example.kotlin_holy.ui.navigation.BackLink
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.LocalShell
import com.example.kotlin_holy.ui.shell.PageWidth
import com.example.kotlin_holy.ui.theme.HolyTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ---------------- Sura betlari ---------------- */

/**
 * Suralar bo'limida sura bosilganda: sura haqida ma'lumot va u joylashgan betlar
 * (Sahifalar bo'limidagi kabi). O'quvchi betni o'zi tanlab kiradi.
 */
@Composable
fun SurahPagesScreen(
    viewModel: QuranListsViewModel = hiltViewModel(),
) {
    val surahNumber = viewModel.routeNumber
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val colors = HolyTheme.colors
    val view = state.settings.listView
    val surah = state.surahs.firstOrNull { it.number == surahNumber }
    val range = JuzCatalog.surahPageRange(surahNumber)
    val pages = remember(state.pages, range) { if (range == null) emptyList() else state.pages.filter { it.number in range } }
    val byPage = remember(state.surahs) { surahsByPage(state.surahs) }
    val colorsByDay = remember(state.xatm) { dayColors(state.xatm) }
    val width = contentWidth(PageWidth.Wide)

    BackLink(Routes.HOME, "Suralar")

    HolyPage(maxWidth = PageWidth.Wide) {
        if (state.error != null) item(key = "error") { AlertBox(state.error) }
        when {
            state.loading -> item(key = "loading") {
                Column {
                    Skeleton(Modifier.fillMaxWidth().height(190.dp).padding(bottom = 24.dp))
                    PageTilesSkeleton(view)
                }
            }
            state.error != null -> Unit
            surah == null || range == null -> item(key = "missing") {
                EmptyState(title = "$surahNumber-sura topilmadi", hint = "Bunday raqamli sura maʼlumot fayllarida yoʻq.")
            }
            else -> {
                item(key = "info") {
                    HolyCard(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            SurahInfo(surah, state.xatm)
                            Divider(Modifier.padding(top = 20.dp))
                            FlowRow(
                                Modifier.fillMaxWidth().padding(top = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                itemVerticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    buildAnnotatedString {
                                        append("Oʻqishni boshlash uchun sahifani tanlang · ")
                                        withStyle(SpanStyle(color = colors.ink, fontWeight = FontWeight.SemiBold)) {
                                            append(if (range.first == range.last) "${range.first}-bet" else "${range.first}–${range.last}-betlar")
                                        }
                                    },
                                    color = colors.inkFaint,
                                    fontSize = 13.sp,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ViewToggle(view, viewModel::setListView)
                                    HolyTextButton("Surani toʻliq oʻqish", { nav.open(Routes.surah(surah.number)) }, size = ButtonSize.Sm)
                                }
                            }
                        }
                    }
                }
                if (pages.isEmpty()) {
                    item(key = "no-pages") {
                        EmptyState(icon = "۩", title = "Bu suraning sahifalari topilmadi", hint = "assets/data/pages.json fayli topilmadi yoki boʻsh.")
                    }
                } else {
                    pageTiles(
                        view = view,
                        pages = pages,
                        byPage = byPage,
                        xatm = state.xatm,
                        colorsByDay = colorsByDay,
                        columns = pageGridColumns(width),
                        wide = width >= 640.dp,
                        onOpen = { nav.open(Routes.surahPage(surah.number, it)) },
                        onToggle = viewModel::togglePage,
                    )
                }
            }
        }
    }
}

/* ---------------- Surani o'qish ---------------- */

data class SurahReadState(
    val loading: Boolean = true,
    val error: String? = null,
    val detail: SurahDetail? = null,
    val groups: List<PageGroup> = emptyList(),
    val cursor: Int = 0,
    val viewMode: String = "ayah",
    val textMode: String = "both",
    val mushaf: Map<Int, MushafPage> = emptyMap(),
    /** Sahifa rejimida tarjima butun bet bo'yicha — bet oyatlari */
    val pageAyahs: Map<Int, List<Ayah>> = emptyMap(),
    val tafsir: Map<String, String> = emptyMap(),
    val saved: Set<String> = emptySet(),
    val xatm: Map<Int, Long> = emptyMap(),
    /** Ochilganda shu oyatga aylantiriladi (?ayah=N) */
    val target: Int? = null,
) {
    val chunks: Int get() = detail?.let { (it.ayahs.size + AYAHS_PER_VIEW - 1) / AYAHS_PER_VIEW } ?: 0
    val visibleAyahs: List<Ayah>
        get() = if (viewMode == "ayah") detail?.ayahs.orEmpty().drop(cursor * AYAHS_PER_VIEW).take(AYAHS_PER_VIEW) else emptyList()
    val group: PageGroup? get() = if (viewMode == "page") groups.getOrNull(cursor) else null
}

@HiltViewModel
class SurahReadViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val xatmRepository: XatmRepository,
    private val ayahSaver: AyahSaver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val number: Int = savedStateHandle.get<String>("number")?.toIntOrNull()?.coerceIn(1, 114) ?: 1
    private val targetAyah: Int? = savedStateHandle.get<String>("ayah")?.toIntOrNull()

    private val base = MutableStateFlow(SurahReadState(target = targetAyah))

    val state: StateFlow<SurahReadState> = combine(
        base,
        settingsRepository.settings,
        ayahSaver.savedVerses,
        xatmRepository.readPages,
    ) { value, settings, saved, xatm ->
        value.copy(textMode = settings.textMode, saved = saved, xatm = xatm)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SurahReadState(target = targetAyah))

    init {
        viewModelScope.launch {
            val mode = settingsRepository.settings.first().viewMode
            val result = runCatching { quranRepository.surah(number) }
            val detail = result.getOrNull()
            base.value = base.value.copy(
                loading = false,
                error = result.exceptionOrNull()?.let { it.message ?: "Nomaʼlum xatolik" },
                detail = detail,
                groups = detail?.let { groupByPage(it.ayahs) }.orEmpty(),
                viewMode = mode,
            )
            if (detail != null) {
                setCursor(cursorFor(mode, targetAyah))
                base.value = base.value.copy(
                    tafsir = runCatching { quranRepository.tafsir(number) }.getOrDefault(emptyMap())
                        .mapKeys { (ayah, _) -> "$number:$ayah" },
                )
            }
        }
    }

    /** Tanlangan oyat qaysi bo'lakda bo'lsa, o'sha bo'lak ochiladi */
    private fun cursorFor(mode: String, ayah: Int?): Int {
        val detail = base.value.detail ?: return 0
        if (ayah == null) return 0
        return if (mode == "ayah") {
            detail.ayahs.indexOfFirst { it.numberInSurah == ayah }.takeIf { it >= 0 }?.div(AYAHS_PER_VIEW) ?: 0
        } else {
            base.value.groups.indexOfFirst { group -> group.ayahs.any { it.numberInSurah == ayah } }.coerceAtLeast(0)
        }
    }

    fun setViewMode(mode: String) {
        viewModelScope.launch { settingsRepository.setViewMode(mode) }
        base.value = base.value.copy(viewMode = mode)
        setCursor(cursorFor(mode, targetAyah))
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }

    fun move(delta: Int) = setCursor(base.value.cursor + delta)

    /** Bo'lak almashganda kerakli mushaf betlari yuklanadi va oxirgi o'qilgan joy yoziladi */
    private fun setCursor(cursor: Int) {
        base.value = base.value.copy(cursor = cursor)
        val current = base.value
        val detail = current.detail ?: return
        val ayahs = if (current.viewMode == "ayah") current.visibleAyahs else current.group?.ayahs.orEmpty()
        val first = ayahs.firstOrNull() ?: return
        viewModelScope.launch {
            settingsRepository.setLastRead(
                Routes.surah(detail.surah.number, first.numberInSurah),
                "${detail.surah.nameUz} ${detail.surah.number}:${first.numberInSurah}",
            )
        }
        val pages = ayahs.mapNotNull { it.pageNumber }.distinct()
        viewModelScope.launch {
            pages.filter { it !in base.value.mushaf }.forEach { page ->
                runCatching { quranRepository.mushafPage(page) }.getOrNull()?.let {
                    base.value = base.value.copy(mushaf = base.value.mushaf + (page to it))
                }
            }
            if (current.viewMode == "page") {
                pages.filter { it !in base.value.pageAyahs }.forEach { page ->
                    runCatching { quranRepository.page(page).ayahs }.getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                        base.value = base.value.copy(pageAyahs = base.value.pageAyahs + (page to it))
                    }
                }
            }
        }
    }

    fun togglePage(page: Int) {
        viewModelScope.launch { xatmRepository.toggle(page) }
    }

    fun toggleSave(ayah: Ayah) {
        viewModelScope.launch { ayahSaver.toggle(ayah) }
    }
}

@Composable
fun SurahReadScreen(
    viewModel: SurahReadViewModel = hiltViewModel(),
    audioViewModel: ReaderAudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    /* QuranNav (tor ekranda), sarlavha kartasi, qiroat kartalari — keyin o'qish qismi */
    val readerIndex = (if (LocalShell.current.wide) 0 else 1) + 2

    BackLink(Routes.HOME, "Suralar")

    val detail = state.detail
    val extras = AyahExtras(state.tafsir, state.saved, viewModel::toggleSave)

    /* ?ayah=N — tanlangan oyat ko'rinadigan joyga aylantiriladi */
    LaunchedEffect(detail != null, state.viewMode) {
        val target = state.target ?: return@LaunchedEffect
        if (detail == null) return@LaunchedEffect
        val index = state.visibleAyahs.indexOfFirst { it.numberInSurah == target }
        if (state.viewMode == "ayah" && index >= 0) listState.scrollToItem(readerIndex + index)
    }

    fun move(delta: Int) {
        viewModel.move(delta)
        scope.launch { listState.animateScrollToItem(readerIndex) }
    }

    HolyPage(state = listState, maxWidth = PageWidth.Reading) {
        when {
            state.loading -> item(key = "loading") { LoadingRows(4) }
            detail == null -> item(key = "error") { AlertBox(state.error ?: "Sura topilmadi") }
            else -> {
                val surah = detail.surah
                item(key = "info") {
                    HolyCard(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            SurahInfo(surah, state.xatm)
                            Divider(Modifier.padding(top = 20.dp))
                            FlowRow(
                                Modifier.padding(top = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Segmented(VIEW_MODES, state.viewMode, viewModel::setViewMode)
                                Segmented(TEXT_MODES, state.textMode, viewModel::setTextMode)
                            }
                        }
                    }
                }
                val group = state.group
                item(key = "audio") {
                    val onScreen = if (state.viewMode == "page") {
                        surahsOf(group?.pageNumber?.let { state.pageAyahs[it] } ?: group?.ayahs.orEmpty())
                    } else {
                        listOf(surah.number to surah.nameUz)
                    }
                    SurahAudioCards(onScreen, audio, audioViewModel, Modifier.padding(bottom = 24.dp))
                }
                if (detail.ayahs.isEmpty()) {
                    item(key = "empty") {
                        EmptyState(title = "Bu surada hali oyat yo'q", hint = "Bu suraning oyatlari maʼlumot fayllarida topilmadi.")
                    }
                } else if (state.viewMode == "ayah") {
                    ayahRows(
                        ayahs = state.visibleAyahs,
                        textMode = state.textMode,
                        mushafPages = state.mushaf,
                        extras = extras,
                        audio = ayahAudio(audio, audioViewModel),
                        surahLink = false,
                        onOpenPage = { nav.open(Routes.page(it)) },
                        onOpenSurah = { nav.open(Routes.surah(it)) },
                    )
                } else if (group != null) {
                    item(key = "page-${group.pageNumber}") {
                        PageGroupCard(
                            caption = (group.pageNumber?.let { "$it-sahifa" } ?: "sahifasiz") +
                                (group.ayahs.firstOrNull()?.juz?.let { " · $it-juz" } ?: ""),
                            textMode = state.textMode,
                            mushaf = group.pageNumber?.let { state.mushaf[it] },
                            translationAyahs = group.pageNumber?.let { state.pageAyahs[it] } ?: group.ayahs,
                            extras = extras,
                            audio = audio,
                            audioViewModel = audioViewModel,
                        )
                    }
                    group.pageNumber?.let { page ->
                        item(key = "mark-$page") { ReadMarkButton(state.xatm[page], onToggle = { viewModel.togglePage(page) }) }
                    }
                }

                if (detail.ayahs.isNotEmpty()) {
                    item(key = "pager") {
                        val number = surah.number
                        if (state.viewMode == "ayah") {
                            val visible = state.visibleAyahs
                            ReaderPager(
                                label = if (visible.isNotEmpty()) "${visible.first().numberInSurah}–${visible.last().numberInSurah} / ${detail.ayahs.size} oyat" else null,
                                prev = when {
                                    state.cursor > 0 -> PagerAction("Oldingi oyatlar") { move(-1) }
                                    number > 1 -> PagerAction("Oldingi sura") { nav.replace(Routes.surah(number - 1)) }
                                    else -> null
                                },
                                next = when {
                                    state.cursor < state.chunks - 1 -> PagerAction("Keyingi oyatlar") { move(1) }
                                    number < 114 -> PagerAction("Keyingi sura") { nav.replace(Routes.surah(number + 1)) }
                                    else -> null
                                },
                            )
                        } else {
                            val page = group?.pageNumber
                            ReaderPager(
                                label = page?.let { "$it-sahifa" },
                                prev = when {
                                    state.cursor > 0 -> PagerAction("Oldingi sahifa") { move(-1) }
                                    page != null && page > 1 -> PagerAction("Oldingi sahifa") { nav.open(Routes.page(page - 1)) }
                                    else -> null
                                },
                                next = when {
                                    state.cursor < state.groups.lastIndex -> PagerAction("Keyingi sahifa") { move(1) }
                                    page != null && page < 604 -> PagerAction("Keyingi sahifa") { nav.open(Routes.page(page + 1)) }
                                    else -> null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
