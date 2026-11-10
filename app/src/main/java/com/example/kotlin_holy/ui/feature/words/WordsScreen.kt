package com.example.kotlin_holy.ui.feature.words

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.HolyInput
import com.example.kotlin_holy.ui.components.ListSkeleton
import com.example.kotlin_holy.ui.components.PageHeader
import com.example.kotlin_holy.ui.components.PagerAction
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.rowBackground
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.LocalShell
import com.example.kotlin_holy.ui.shell.PageWidth
import com.example.kotlin_holy.ui.theme.AmiriQuran
import com.example.kotlin_holy.ui.theme.HolyTheme
import kotlinx.coroutines.launch

/* Tailwind chegaralari: sm — 640px, md — 768px, lg — 1024px */
private val SM = 640.dp
private val MD = 768.dp
private val LG = 1024.dp

/** /words — Qur'ondagi o'qilishi eng og'ir so'zlar: qidiruv, katak / ro'yxat, sahifalash */
@Composable
fun WordsScreen() {
    val viewModel: WordsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    /* Ro'yxat boshi: tor ekranda HolyPage tepasida «Qur'on bo'ylab o'tish» qatori ham bor */
    val anchorIndex = (if (LocalShell.current.wide) 0 else 1) + 1

    fun movePage(delta: Int) {
        viewModel.movePage(delta)
        scope.launch { listState.animateScrollToItem(anchorIndex) }
    }

    val openWord = { word: WordEntry ->
        word.firstSurah?.let { surah -> nav.open(Routes.surah(surah, word.firstAyah)) }
        Unit
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sm = maxWidth >= SM
        val md = maxWidth >= MD
        val columns = when {
            maxWidth >= LG -> 3
            sm -> 2
            else -> 1
        }
        HolyPage(state = listState, maxWidth = PageWidth.Section) {
            item(key = "header") {
                Column(Modifier.fillMaxWidth()) {
                    PageHeader(
                        title = "Qiyin soʻzlar",
                        subtitle = "Qur'ondagi oʻqilishi eng ogʻir ${state.total} ta soʻz — harfi koʻp, madd va hamzali kalimalar",
                        trailing = {
                            Row(
                                Modifier.then(if (sm) Modifier else Modifier.fillMaxWidth()),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                HolyInput(
                                    value = viewModel.queryText,
                                    onValueChange = viewModel::setQuery,
                                    placeholder = "Soʻz qidirish…",
                                    leadingIcon = HolyIcons.Search,
                                    modifier = if (sm) Modifier.width(240.dp) else Modifier.weight(1f),
                                )
                                ViewToggle(view = state.view, onChange = viewModel::setView)
                            }
                        },
                    )
                    Text(
                        "Har bir soʻzning yonida lotincha oʻqilishi berilgan. Soʻzni bossangiz, u birinchi " +
                            "marta uchragan oyatga oʻtasiz va oʻzbekcha tarjimasini koʻrasiz.",
                        color = HolyTheme.colors.inkFaint,
                        fontSize = 13.sp,
                        lineHeight = 21.sp,
                        modifier = Modifier.widthIn(max = 672.dp).padding(bottom = 24.dp),
                    )
                    AlertBox(state.error)
                }
            }
            /* Sahifa almashganda shu yerga suriladi */
            item(key = "anchor") { Box(Modifier.fillMaxWidth()) }

            when {
                state.loading -> if (state.view == "list") {
                    item(key = "skeleton") { ListSkeleton(rows = 12) }
                } else {
                    val rows = (0 until 9).chunked(columns)
                    itemsIndexed(rows, key = { index, _ -> "skeleton:$columns:$index" }) { index, row ->
                        WordGridRow(row, columns, Modifier.padding(top = if (index > 0) 12.dp else 0.dp)) { _, modifier, _ ->
                            Skeleton(modifier.height(124.dp))
                        }
                    }
                }

                state.items.isEmpty() -> item(key = "empty") {
                    EmptyState(
                        title = if (state.search.isNotEmpty()) "Hech narsa topilmadi" else "Roʻyxat boʻsh",
                        hint = if (state.search.isNotEmpty()) "Boshqa soʻz bilan qidirib koʻring." else null,
                    )
                }

                else -> {
                    val items = state.items
                    if (state.view == "list") {
                        itemsIndexed(items, key = { _, word -> "word:${word.id}" }) { index, word ->
                            WordRow(word, index, items.size, md) { openWord(word) }
                        }
                    } else {
                        val rows = items.chunked(columns)
                        itemsIndexed(rows, key = { index, row -> "cards:$columns:$index:${row.first().id}" }) { index, row ->
                            WordGridRow(row, columns, Modifier.padding(top = if (index > 0) 12.dp else 0.dp)) { word, modifier, stretched ->
                                WordCard(word, stretched, modifier) { openWord(word) }
                            }
                        }
                    }

                    item(key = "pager") {
                        ReaderPager(
                            label = "${state.pageIndex + 1} / ${state.totalPages}",
                            prev = if (state.pageIndex > 0) PagerAction("Oldingi") { movePage(-1) } else null,
                            next = if (state.pageIndex < state.totalPages - 1) PagerAction("Keyingi") { movePage(1) } else null,
                        )
                    }
                    item(key = "range") {
                        val from = state.pageIndex * PAGE_SIZE + 1
                        Text(
                            "$from–${state.pageIndex * PAGE_SIZE + items.size} / ${state.total} soʻz",
                            color = HolyTheme.colors.inkFaint,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Qiyinlik: uch nuqta — 90 dan yuqori juda qiyin, 80 dan qiyin, qolgani o'rtacha */
@Composable
private fun DifficultyDots(score: Int) {
    val colors = HolyTheme.colors
    val level = when {
        score >= 90 -> 3
        score >= 80 -> 2
        else -> 1
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        (1..3).forEach { dot ->
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (dot <= level) colors.accent else colors.line),
            )
        }
    }
}

/** Jadval uslubidagi qator: o'qilishi · oyat · (keng ekranda) necha marta · qiyinlik · arabcha so'z */
@Composable
private fun WordRow(word: WordEntry, index: Int, count: Int, md: Boolean, onOpen: () -> Unit) {
    val colors = HolyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .listBoxRow(index, count)
            .background(rowBackground(index))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                word.transliteration?.takeIf { it.isNotEmpty() } ?: "—",
                color = colors.accent,
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${word.firstSurah}:${word.firstAyah}-oyat →" + if (md) "" else " · ${word.times} marta",
                color = colors.inkFaint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (md) {
            Box(Modifier.width(88.dp)) { HolyBadge("${word.times} marta") }
            Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) { DifficultyDots(word.difficulty) }
        }
        Text(
            word.text,
            style = TextStyle(
                fontFamily = AmiriQuran,
                fontSize = 24.sp,
                lineHeight = 43.sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right,
                color = colors.ink,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (md) Modifier.width(160.dp) else Modifier,
        )
    }
}

/** Katak: arabcha so'z, o'qilishi, ostida oyat, necha marta va qiyinlik */
@Composable
private fun WordCard(word: WordEntry, stretched: Boolean, modifier: Modifier, onOpen: () -> Unit) {
    val colors = HolyTheme.colors
    HolyCard(modifier = modifier, onClick = onOpen) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (stretched) Modifier.fillMaxHeight() else Modifier)
                .padding(20.dp),
        ) {
            Text(
                word.text,
                style = TextStyle(
                    fontFamily = AmiriQuran,
                    fontSize = 30.sp,
                    lineHeight = 57.sp,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right,
                    color = colors.ink,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!word.transliteration.isNullOrEmpty()) {
                Text(
                    word.transliteration,
                    color = colors.accent,
                    fontSize = 15.sp,
                    lineHeight = 22.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            /* mt-auto: qatordagi kataklar teng balandlikda bo'lsa, past qism tagida turadi */
            if (stretched) Spacer(Modifier.weight(1f))
            Divider()
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "${word.firstSurah}:${word.firstAyah}-oyat →",
                    color = colors.inkFaint,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.weight(1f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    HolyBadge("${word.times} marta")
                    DifficultyDots(word.difficulty)
                }
            }
        }
    }
}

/* ---------------- Yordamchi joylashuvlar ---------------- */

/** To'rning bitta qatori: kataklar teng kenglikda va balandlikda; bitta ustunda — to'liq kenglikda */
@Composable
private fun <T> WordGridRow(
    cells: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    gap: Dp = 12.dp,
    cell: @Composable (item: T, modifier: Modifier, stretched: Boolean) -> Unit,
) {
    if (columns <= 1) {
        Column(modifier.fillMaxWidth()) {
            cells.forEach { cell(it, Modifier.fillMaxWidth(), false) }
        }
        return
    }
    Row(
        modifier.fillMaxWidth().height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(gap),
    ) {
        cells.forEach { cell(it, Modifier.weight(1f).fillMaxHeight(), true) }
        repeat(columns - cells.size) { Spacer(Modifier.weight(1f)) }
    }
}

/**
 * LIST_BOX qatori — qatorlar LazyColumn'da alohida chiziladi: birinchisida yuqori, oxirgisida
 * pastki burchaklar yumaloq; hoshiya butun qutini o'raydi (qatorlar orasida chiziq yo'q).
 */
@Composable
private fun Modifier.listBoxRow(index: Int, count: Int): Modifier {
    val radius = HolyTheme.radius
    val line = HolyTheme.colors.line
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    return this
        .clip(shape)
        .drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            val corner = radius.toPx()
            /* Ichki tomonlar qator chegarasidan tashqariga chiqariladi — kesilib, ko'rinmaydi */
            val overflow = corner * 2 + stroke
            val top = if (first) stroke / 2 else -overflow
            val bottom = if (last) size.height - stroke / 2 else size.height + overflow
            drawRoundRect(
                color = line,
                topLeft = Offset(stroke / 2, top),
                size = Size(size.width - stroke, bottom - top),
                cornerRadius = CornerRadius((corner - stroke / 2).coerceAtLeast(0f)),
                style = Stroke(stroke),
            )
        }
}
