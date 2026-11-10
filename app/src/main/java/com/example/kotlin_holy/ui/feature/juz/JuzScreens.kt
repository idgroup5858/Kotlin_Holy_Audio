package com.example.kotlin_holy.ui.feature.juz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.kotlin_holy.domain.model.JuzInfo
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.AyahExtras
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.PageHeader
import com.example.kotlin_holy.ui.components.PageTilesSkeleton
import com.example.kotlin_holy.ui.components.PagerAction
import com.example.kotlin_holy.ui.components.ReadStatus
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.Segmented
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.dayColors
import com.example.kotlin_holy.ui.components.listBoxSegment
import com.example.kotlin_holy.ui.components.pageGridColumns
import com.example.kotlin_holy.ui.components.rowBackground
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

/** Juzning suralari: «Baqara» yoki «Baqara — Oli Imron» */
private fun juzNames(juz: JuzInfo, names: Map<Int, String>): String? {
    val list = listOfNotNull(names[juz.fromSurah], names[juz.toSurah])
    if (list.isEmpty()) return null
    return if (juz.fromSurah == juz.toSurah) list.first() else list.joinToString(" — ")
}

/* ---------------- Juzlar ro'yxati ---------------- */

@Composable
fun JuzListScreen(viewModel: QuranListsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val colors = HolyTheme.colors
    val view = state.settings.listView
    val names = remember(state.surahs) { state.surahs.associate { it.number to it.nameUz } }

    HolyPage(maxWidth = PageWidth.Section) {
        item(key = "header") {
            PageHeader(
                title = "Juzlar",
                subtitle = "Qurʼonning 30 juzi — har biri qaysi suralar, oyatlar va betlarni oʻz ichiga oladi.",
            ) { ViewToggle(view, viewModel::setListView) }
        }
        if (view == "list") {
            val all = JuzCatalog.all
            items(all.size, key = { "j${all[it].number}" }) { index ->
                val juz = all[index]
                Row(
                    Modifier
                        .listBoxSegment(first = index == 0, last = index == all.lastIndex)
                        .background(rowBackground(index))
                        .clickable { nav.open(Routes.juzPages(juz.number)) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("${juz.number}", color = colors.inkFaint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            juzNames(juz, names) ?: "${juz.number}-juz",
                            color = colors.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${juz.fromSurah}:${juz.fromAyah} – ${juz.toSurah}:${juz.toAyah} · ${juz.fromPage}–${juz.toPage}-bet",
                            color = colors.inkFaint,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("${juz.ayahCount} oyat", color = colors.inkFaint, fontSize = 12.sp, textAlign = TextAlign.End)
                }
            }
        } else {
            item(key = "grid") {
                BoxWithConstraints {
                    val columns = when {
                        maxWidth >= 1024.dp -> 5
                        maxWidth >= 640.dp -> 4
                        else -> 2
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        JuzCatalog.all.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { juz ->
                                    HolyCard(Modifier.weight(1f), onClick = { nav.open(Routes.juzPages(juz.number)) }) {
                                        Column(
                                            Modifier.fillMaxWidth().heightIn(min = 140.dp).padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                                        ) {
                                            Text("${juz.number}", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                                            juzNames(juz, names)?.let {
                                                Text(it, color = colors.inkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                            }
                                            Text("${juz.fromSurah}:${juz.fromAyah} – ${juz.toSurah}:${juz.toAyah}", color = colors.inkFaint, fontSize = 11.sp)
                                            Text(
                                                "${juz.fromPage}–${juz.toPage}-bet",
                                                color = colors.accent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .clip(HolyTheme.shape)
                                                    .background(colors.accent.copy(alpha = 0.10f))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                            )
                                            Text("${juz.ayahCount} oyat", color = colors.inkFaint, fontSize = 11.sp)
                                        }
                                    }
                                }
                                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- Juz betlari ---------------- */

/** Juz sarlavhasi: raqami, qaysi suralardan iborati, oyatlar oralig'i va o'qilgan betlar */
@Composable
private fun JuzInfoBlock(juz: JuzInfo, names: Map<Int, String>, xatm: Map<Int, Long>) {
    val colors = HolyTheme.colors
    val status = ReadStatus((juz.fromPage..juz.toPage).count { xatm.containsKey(it) }, juz.toPage - juz.fromPage + 1)
    BoxWithConstraints {
        val wide = maxWidth >= 600.dp
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (wide) {
                Box(
                    Modifier.height(64.dp).widthIn(min = 64.dp).clip(HolyTheme.shape).background(colors.surface2).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${juz.number}", color = colors.accent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("${juz.number}-juz", color = colors.ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
                juzNames(juz, names)?.let {
                    Text(it, color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    "${juz.fromSurah}:${juz.fromAyah} – ${juz.toSurah}:${juz.toAyah} oyatlarni oʻz ichiga oladi, ${juz.ayahCount} oyatdan iborat. " +
                        "Mushafning ${juz.fromPage}–${juz.toPage}-sahifalarida (${juz.toPage - juz.fromPage + 1} bet) joylashgan.",
                    color = colors.inkFaint,
                    fontSize = 13.sp,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                FlowRow(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HolyBadge("${juz.ayahCount} oyat", tone = BadgeTone.Accent)
                    HolyBadge("${juz.fromPage}-sahifadan")
                    HolyBadge(
                        if (status.done) "✓ Toʻliq oʻqildi · ${status.total} bet"
                        else "${status.read}/${status.total} bet oʻqildi · ${status.total - status.read} ta qoldi",
                        tone = BadgeTone.Read,
                    )
                }
            }
        }
    }
}

/**
 * Juzlar bo'limida juz bosilganda: juz haqida ma'lumot va u joylashgan betlar
 * (sura betlari kabi). O'quvchi betni o'zi tanlab kiradi.
 */
@Composable
fun JuzPagesScreen(viewModel: QuranListsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val colors = HolyTheme.colors
    val number = viewModel.routeNumber
    val juz = JuzCatalog.byNumber(number)
    val view = state.settings.listView
    val names = remember(state.surahs) { state.surahs.associate { it.number to it.nameUz } }
    val pages = remember(state.pages, juz) { if (juz == null) emptyList() else state.pages.filter { it.number in juz.fromPage..juz.toPage } }
    val byPage = remember(state.surahs) { surahsByPage(state.surahs) }
    val colorsByDay = remember(state.xatm) { dayColors(state.xatm) }
    val width = contentWidth(PageWidth.Wide)

    BackLink(Routes.JUZ_LIST, "Juzlar")

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
            juz == null -> item(key = "missing") {
                EmptyState(title = "$number-juz topilmadi", hint = "Qurʼon 30 juzdan iborat — 1 dan 30 gacha raqam tanlang.")
            }
            else -> {
                item(key = "info") {
                    HolyCard(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            JuzInfoBlock(juz, names, state.xatm)
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
                                            append("${juz.fromPage}–${juz.toPage}-betlar")
                                        }
                                    },
                                    color = colors.inkFaint,
                                    fontSize = 13.sp,
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ViewToggle(view, viewModel::setListView)
                                    HolyTextButton("Juzni toʻliq oʻqish", { nav.open(Routes.juz(juz.number)) }, size = ButtonSize.Sm)
                                }
                            }
                        }
                    }
                }
                if (pages.isEmpty()) {
                    item(key = "no-pages") {
                        EmptyState(icon = "۩", title = "Bu juzning sahifalari topilmadi", hint = "assets/data/pages.json fayli topilmadi yoki boʻsh.")
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
                        onOpen = { nav.open(Routes.juzPage(juz.number, it)) },
                        onToggle = viewModel::togglePage,
                    )
                }
            }
        }
    }
}

/* ---------------- Juzni o'qish ---------------- */

data class JuzReadState(
    val number: Int,
    val loading: Boolean = true,
    val error: String? = null,
    val ayahs: List<Ayah> = emptyList(),
    val groups: List<PageGroup> = emptyList(),
    val cursor: Int = 0,
    val viewMode: String = "ayah",
    val textMode: String = "both",
    val mushaf: Map<Int, MushafPage> = emptyMap(),
    val pageAyahs: Map<Int, List<Ayah>> = emptyMap(),
    val tafsir: Map<String, String> = emptyMap(),
    val saved: Set<String> = emptySet(),
) {
    val chunks: Int get() = (ayahs.size + AYAHS_PER_VIEW - 1) / AYAHS_PER_VIEW
    val visibleAyahs: List<Ayah>
        get() = if (viewMode == "ayah") ayahs.drop(cursor * AYAHS_PER_VIEW).take(AYAHS_PER_VIEW) else emptyList()
    val group: PageGroup? get() = if (viewMode == "page") groups.getOrNull(cursor) else null
}

@HiltViewModel
class JuzReadViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val ayahSaver: AyahSaver,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val number: Int = savedStateHandle.get<String>("number")?.toIntOrNull()?.coerceIn(1, 30) ?: 1
    private val base = MutableStateFlow(JuzReadState(number))

    val state: StateFlow<JuzReadState> = combine(base, settingsRepository.settings, ayahSaver.savedVerses) { value, settings, saved ->
        value.copy(textMode = settings.textMode, saved = saved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JuzReadState(number))

    /* Tafsir sura-ba-sura, kerak bo'lganda yuklanadi */
    private val tafsirLoaded = HashSet<Int>()

    init {
        viewModelScope.launch {
            val mode = settingsRepository.settings.first().viewMode
            val result = runCatching { quranRepository.juzAyahs(number) }
            val ayahs = result.getOrDefault(emptyList())
            base.value = base.value.copy(
                loading = false,
                error = result.exceptionOrNull()?.let { it.message ?: "Nomaʼlum xatolik" },
                ayahs = ayahs,
                groups = groupByPage(ayahs),
                viewMode = mode,
            )
            if (ayahs.isNotEmpty()) {
                settingsRepository.setLastRead(Routes.juz(number), "$number-juz")
                setCursor(0)
            }
        }
    }

    fun setViewMode(mode: String) {
        viewModelScope.launch { settingsRepository.setViewMode(mode) }
        base.value = base.value.copy(viewMode = mode)
        setCursor(0)
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }

    fun move(delta: Int) = setCursor(base.value.cursor + delta)

    private fun setCursor(cursor: Int) {
        base.value = base.value.copy(cursor = cursor)
        val current = base.value
        val ayahs = if (current.viewMode == "ayah") current.visibleAyahs else current.group?.ayahs.orEmpty()
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
            val surahs = (ayahs + pages.flatMap { base.value.pageAyahs[it].orEmpty() }).map { it.surahNumber }.distinct()
            surahs.filter { tafsirLoaded.add(it) }.forEach { surah ->
                val map = runCatching { quranRepository.tafsir(surah) }.getOrDefault(emptyMap())
                base.value = base.value.copy(tafsir = base.value.tafsir + map.mapKeys { (ayah, _) -> "$surah:$ayah" })
            }
        }
    }

    fun toggleSave(ayah: Ayah) {
        viewModelScope.launch { ayahSaver.toggle(ayah) }
    }
}

@Composable
fun JuzReadScreen(
    viewModel: JuzReadViewModel = hiltViewModel(),
    audioViewModel: ReaderAudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val colors = HolyTheme.colors
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val current = state.number
    /* QuranNav (tor ekranda), sarlavha, rejimlar, qiroat kartalari — keyin o'qish qismi */
    val readerIndex = (if (LocalShell.current.wide) 0 else 1) + 3
    val extras = AyahExtras(state.tafsir, state.saved, viewModel::toggleSave)

    fun move(delta: Int) {
        viewModel.move(delta)
        scope.launch { listState.animateScrollToItem(readerIndex) }
    }

    HolyPage(state = listState, maxWidth = PageWidth.Reading) {
        item(key = "header") {
            FlowRow(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("$current-juz", color = colors.ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp)
                    Text(
                        if (state.loading) "Yuklanmoqda…" else "${state.ayahs.size} ta oyat",
                        color = colors.inkFaint,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                HolyTextButton("Barcha juzlar", { nav.back(Routes.JUZ_LIST) }, size = ButtonSize.Sm)
            }
        }
        item(key = "modes") {
            FlowRow(
                Modifier.padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Segmented(VIEW_MODES, state.viewMode, viewModel::setViewMode)
                Segmented(TEXT_MODES, state.textMode, viewModel::setTextMode)
            }
        }
        val group = state.group
        item(key = "audio") {
            val onScreen = if (state.viewMode == "page") {
                surahsOf(group?.pageNumber?.let { state.pageAyahs[it] } ?: group?.ayahs.orEmpty())
            } else {
                surahsOf(state.visibleAyahs)
            }
            SurahAudioCards(onScreen, audio, audioViewModel, Modifier.padding(bottom = 20.dp))
        }
        if (state.error != null) item(key = "error") { AlertBox(state.error) }
        when {
            state.loading -> item(key = "loading") { LoadingRows(3) }
            state.ayahs.isEmpty() -> item(key = "empty") {
                EmptyState(title = "$current-juzda oyat topilmadi", hint = "Bu juz boʻyicha maʼlumot fayllarda topilmadi.")
            }
            state.viewMode == "ayah" -> ayahRows(
                ayahs = state.visibleAyahs,
                textMode = state.textMode,
                mushafPages = state.mushaf,
                extras = extras,
                audio = ayahAudio(audio, audioViewModel),
                surahLink = true,
                onOpenPage = { nav.open(Routes.page(it)) },
                onOpenSurah = { nav.open(Routes.surah(it)) },
            )
            group != null -> item(key = "page-${group.pageNumber}") {
                PageGroupCard(
                    caption = "${group.pageNumber?.let { "$it-sahifa" } ?: "sahifasiz"} · $current-juz",
                    textMode = state.textMode,
                    mushaf = group.pageNumber?.let { state.mushaf[it] },
                    translationAyahs = group.pageNumber?.let { state.pageAyahs[it] } ?: group.ayahs,
                    extras = extras,
                    audio = audio,
                    audioViewModel = audioViewModel,
                )
            }
        }
        if (!state.loading && state.ayahs.isNotEmpty()) {
            item(key = "pager") {
                if (state.viewMode == "ayah") {
                    val visible = state.visibleAyahs
                    ReaderPager(
                        label = if (visible.isNotEmpty()) "${state.cursor * AYAHS_PER_VIEW + 1}–${state.cursor * AYAHS_PER_VIEW + visible.size} / ${state.ayahs.size} oyat" else null,
                        prev = when {
                            state.cursor > 0 -> PagerAction("Oldingi oyatlar") { move(-1) }
                            current > 1 -> PagerAction("Oldingi juz") { nav.replace(Routes.juz(current - 1)) }
                            else -> null
                        },
                        next = when {
                            state.cursor < state.chunks - 1 -> PagerAction("Keyingi oyatlar") { move(1) }
                            current < 30 -> PagerAction("Keyingi juz") { nav.replace(Routes.juz(current + 1)) }
                            else -> null
                        },
                    )
                } else {
                    ReaderPager(
                        label = group?.pageNumber?.let { "$it-sahifa" },
                        prev = when {
                            state.cursor > 0 -> PagerAction("Oldingi sahifa") { move(-1) }
                            current > 1 -> PagerAction("Oldingi juz") { nav.replace(Routes.juz(current - 1)) }
                            else -> null
                        },
                        next = when {
                            state.cursor < state.groups.lastIndex -> PagerAction("Keyingi sahifa") { move(1) }
                            current < 30 -> PagerAction("Keyingi juz") { nav.replace(Routes.juz(current + 1)) }
                            else -> null
                        },
                    )
                }
            }
        }
    }
}
