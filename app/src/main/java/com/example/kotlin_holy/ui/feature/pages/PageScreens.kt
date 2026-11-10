package com.example.kotlin_holy.ui.feature.pages

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyInput
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.PageHeader
import com.example.kotlin_holy.ui.components.PageListRow
import com.example.kotlin_holy.ui.components.PageSurahs
import com.example.kotlin_holy.ui.components.PageTileGrid
import com.example.kotlin_holy.ui.components.PageTilesSkeleton
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.dayColorOf
import com.example.kotlin_holy.ui.components.dayColors
import com.example.kotlin_holy.ui.components.listBoxSegment
import com.example.kotlin_holy.ui.components.pageGridColumns
import com.example.kotlin_holy.ui.components.surahsByPage
import com.example.kotlin_holy.ui.feature.home.OrderToggle
import com.example.kotlin_holy.ui.feature.home.QuranListsViewModel
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.PageWidth

/** Kontent kengligi: ekran − yon panel − chetlar, sahifa kengligidan oshmaydi */
@Composable
fun contentWidth(maxWidth: Dp): Dp {
    val screen = LocalConfiguration.current.screenWidthDp.dp
    val sidebar = if (screen >= 1024.dp) 241.dp else 0.dp
    return minOf(screen - sidebar - 32.dp, maxWidth)
}

/**
 * Sahifalar katak yoki ro'yxat ko'rinishida — LazyColumn qatorlari bo'lib (604 bet ham silliq).
 * Sahifalar bo'limi, sura va juz betlari uchun umumiy.
 */
fun LazyListScope.pageTiles(
    view: String,
    pages: List<PageSummary>,
    byPage: Map<Int, PageSurahs>,
    xatm: Map<Int, Long>,
    colorsByDay: Map<String, Color>,
    columns: Int,
    wide: Boolean,
    onOpen: (Int) -> Unit,
    onToggle: (Int) -> Unit,
) {
    if (view == "list") {
        items(pages.size, key = { "p${pages[it].number}" }) { index ->
            val page = pages[index]
            Box(Modifier.listBoxSegment(first = index == 0, last = index == pages.lastIndex)) {
                PageListRow(
                    page = page,
                    index = index,
                    info = byPage[page.number],
                    readAt = xatm[page.number],
                    dayColor = dayColorOf(xatm[page.number], colorsByDay),
                    wide = wide,
                    onOpen = onOpen,
                    onToggle = onToggle,
                )
            }
        }
    } else {
        val rows = pages.chunked(columns)
        items(rows.size, key = { "r${rows[it].first().number}" }) { index ->
            PageTileGrid(
                pages = rows[index],
                byPage = byPage,
                xatm = xatm,
                dayColors = colorsByDay,
                onOpen = onOpen,
                onToggle = onToggle,
                columns = columns,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }
}

/** /pages — barcha 604 bet */
@Composable
fun PageListScreen(viewModel: QuranListsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val view = state.settings.listView
    val order = state.settings.pageOrder
    var jump by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val width = contentWidth(PageWidth.Wide)
    val columns = pageGridColumns(width)

    val byPage = remember(state.surahs) { surahsByPage(state.surahs) }
    val colorsByDay = remember(state.xatm) { dayColors(state.xatm) }
    val visible = remember(state.pages, order) {
        if (order == "asc") state.pages.sortedBy { it.number } else state.pages.sortedByDescending { it.number }
    }

    fun go() {
        val target = jump.trim().toIntOrNull()
        if (target != null && target in 1..604) nav.open(Routes.page(target))
    }

    /* Bo'limga kirganda oxirgi belgilangan bet ekran o'rtasiga keladi — faqat bir marta */
    var scrolled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.loading, visible, view) {
        if (state.loading || visible.isEmpty() || scrolled) return@LaunchedEffect
        scrolled = true
        val last = state.xatm.maxByOrNull { (page, at) -> at * 1000 + page }?.key ?: return@LaunchedEffect
        val position = visible.indexOfFirst { it.number == last }.takeIf { it >= 0 } ?: return@LaunchedEffect
        /* QuranNav va sarlavhadan keyin */
        val target = (if (view == "list") position else position / columns) + 2
        listState.scrollToItem(target)
        val info = listState.layoutInfo
        val itemSize = info.visibleItemsInfo.firstOrNull { it.index == target }?.size ?: 0
        listState.scrollBy(-(info.viewportSize.height / 2 - itemSize / 2).toFloat())
    }

    HolyPage(state = listState, maxWidth = PageWidth.Wide) {
        item(key = "header") {
            PageHeader(
                title = "Sahifalar",
                subtitle = if (state.loading) "Yuklanmoqda…" else "${state.pages.size} ta sahifada maʼlumot bor · mushaf boʻyicha 1–604",
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    OrderToggle(order) { viewModel.setPageOrder(if (order == "asc") "desc" else "asc") }
                    ViewToggle(view, viewModel::setListView)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        HolyInput(
                            value = jump,
                            onValueChange = { value -> jump = value.filter { it.isDigit() }.take(3) },
                            placeholder = "Sahifa №",
                            textStyle = TextStyle(fontSize = 14.sp, textAlign = TextAlign.Center),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                            modifier = Modifier.width(160.dp),
                        )
                        HolyTextButton("O'tish", ::go, variant = ButtonVariant.Primary)
                    }
                }
            }
        }
        if (state.error != null) item(key = "error") { AlertBox(state.error, Modifier.padding(bottom = 16.dp)) }
        when {
            state.loading -> item(key = "loading") { PageTilesSkeleton(view) }
            state.pages.isEmpty() -> item(key = "empty") {
                EmptyState(
                    icon = "۩",
                    title = "Hozircha sahifa maʼlumoti yoʻq",
                    hint = "assets/data/pages.json fayli topilmadi yoki boʻsh.",
                )
            }
            else -> pageTiles(
                view = view,
                pages = visible,
                byPage = byPage,
                xatm = state.xatm,
                colorsByDay = colorsByDay,
                columns = columns,
                wide = width >= 640.dp,
                onOpen = { nav.open(Routes.page(it)) },
                onToggle = viewModel::togglePage,
            )
        }
    }
}
