package com.example.kotlin_holy.ui.feature.surahs

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.MushafWord
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyTextField
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.MushafFontProvider
import com.example.kotlin_holy.ui.components.OrderToggle
import com.example.kotlin_holy.ui.components.ReadCheckCircle
import com.example.kotlin_holy.ui.theme.HolyTheme

@Composable
fun SurahListScreen(
    onOpenSurah: (Int) -> Unit,
    viewModel: SurahListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            HighlightAyahsCard(viewModel = viewModel)
        }

        item {
            HolyTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Sura qidirish…",
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.inkFaint,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.visible.size} ta sura",
                    color = colors.inkFaint,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                OrderToggle(order = state.order, onToggle = viewModel::toggleOrder)
            }
        }

        if (state.loading) {
            item { LoadingRows(rows = 6) }
        } else if (state.visible.isEmpty()) {
            item {
                EmptyState(
                    title = "Hech narsa topilmadi",
                    hint = "Boshqa soʻz bilan qidirib koʻring.",
                )
            }
        } else {
            items(state.visible, key = { it.number }) { surah ->
                SurahRow(
                    surah = surah,
                    xatm = state.xatm[surah.number],
                    onClick = { onOpenSurah(surah.number) },
                    onToggle = { viewModel.toggleSurah(surah.number) },
                )
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: Surah,
    xatm: SurahXatm?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val done = xatm?.done == true

    HolyCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (done) colors.read.copy(alpha = 0.7f) else null,
        background = if (done) colors.read.copy(alpha = 0.07f) else null,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                /* Raqam romb ichida — veb nusxadagi Diamond */
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .rotate(45f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = surah.number.toString(),
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.rotate(-45f),
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        surah.nameUz,
                        color = colors.ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        surah.meaningUz ?: if (surah.isMakki) "Makkiy" else "Madaniy",
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (xatm != null && xatm.started) {
                        Text(
                            text = if (xatm.done) "✓ Oʻqildi" else "${xatm.read}/${xatm.total} bet oʻqildi",
                            color = colors.read,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                /* O'ng burchakdagi belgi doirasi bilan to'qnashmasligi uchun chekinish */
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 20.dp),
                ) {
                    Text(
                        surah.nameArabic,
                        color = colors.ink,
                        fontSize = 18.sp,
                        maxLines = 1,
                    )
                    Text(
                        "${surah.totalAyahs} oyat",
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            ReadCheckCircle(
                checked = done,
                partial = xatm?.started == true && !done,
                onToggle = onToggle,
                size = 22.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}

private data class HighlightTab(val key: String, val label: String)

private val highlightTabs = listOf(
    HighlightTab("anbiyo", "Anbiyo surasi"),
    HighlightTab("zuxruf", "Zuxruf surasi"),
    HighlightTab("kursiy", "Oyat al-Kursiy"),
    HighlightTab("amanarrasul", "Amanar-Rasul"),
)

/**
 * Bosh sahifada Suralar ro'yxati tepasida chiqadigan tanlangan oyatlar
 * kartasi. Tab bosilganda yangi ekranga o'tilmaydi — matn shu joyning
 * o'zida almashadi (web nusxadagi kabi yengil, "collapse" uslubida).
 */
@Composable
private fun HighlightAyahsCard(viewModel: SurahListViewModel) {
    val colors = HolyTheme.colors
    val ayahs by viewModel.highlightAyahs.collectAsStateWithLifecycle()
    val selected by viewModel.highlightTab.collectAsStateWithLifecycle()

    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                highlightTabs.forEach { tab ->
                    val active = tab.key == selected
                    Text(
                        text = tab.label,
                        color = if (active) colors.bg else colors.inkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) colors.accent else colors.surface2)
                            .clickable { viewModel.selectHighlightTab(tab.key) }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            Column(Modifier.padding(top = 12.dp)) {
                when (selected) {
                    "kursiy" -> MushafAyahSnippet(
                        verseKeys = setOf("2:255"),
                        pages = ayahs.kursiyPages,
                    )
                    "amanarrasul" -> MushafAyahSnippet(
                        verseKeys = setOf("2:285", "2:286"),
                        pages = ayahs.amanarrasulPages,
                    )
                    "zuxruf" -> ayahs.zuxruf.forEach { HighlightTranslationLine(it) }
                    else -> ayahs.anbiyo.forEach { HighlightTranslationLine(it) }
                }
            }
        }
    }
}

/** Sura nomi va oyat raqami bilan birga chiqadigan o'zbekcha tarjima qatori */
@Composable
private fun HighlightTranslationLine(ayah: Ayah) {
    val colors = HolyTheme.colors
    Text(
        text = "${ayah.numberInSurah}. ${ayah.translationUz ?: ""}",
        color = colors.inkSoft,
        fontSize = 13.sp,
        lineHeight = 22.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

private const val SNIPPET_BASE_FONT_PX = 100f

private data class SnippetLine(
    val words: List<MushafWord>,
    val family: FontFamily,
    val typeface: Typeface,
)

/**
 * Arabcha oyatni aynan bosma mushaf sahifasidagi shrift, satr bo'linishi va
 * kenglikka moslash mantig'i bilan chizadi (MushafPageView'dagi mexanizmning
 * o'zi) — shuning uchun natija bosma kitobdagi o'sha oyat qanday tursa,
 * xuddi shunday ko'chirib qo'yilgandek chiqadi.
 */
@Composable
private fun MushafAyahSnippet(verseKeys: Set<String>, pages: List<MushafPage>) {
    val colors = HolyTheme.colors
    val context = LocalContext.current
    val density = LocalDensity.current

    val lines = remember(pages, verseKeys) {
        pages.flatMap { page ->
            val typeface = MushafFontProvider.typeface(context, page.pageNumber)
            val family = MushafFontProvider.family(context, page.pageNumber)
            page.lines.mapNotNull { line ->
                val words = line.words.filter { it.verseKey in verseKeys }
                if (words.isEmpty()) null else SnippetLine(words, family, typeface)
            }
        }
    }
    if (lines.isEmpty()) return

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availablePx = with(density) { maxWidth.toPx() }
        val fontSizeSp = remember(lines, availablePx) {
            val widest = lines.maxOf { snippetLine ->
                val paint = Paint().apply {
                    typeface = snippetLine.typeface
                    textSize = SNIPPET_BASE_FONT_PX
                    isAntiAlias = true
                }
                paint.measureText(snippetLine.words.joinToString("") { it.code })
            }
            val fontPx = (availablePx * 0.99f / widest * SNIPPET_BASE_FONT_PX).coerceIn(10f, 200f)
            with(density) { fontPx.toSp().value }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth()) {
                lines.forEach { snippetLine ->
                    val text = buildAnnotatedString {
                        snippetLine.words.forEach { word ->
                            withStyle(SpanStyle(color = if (word.isEnd) colors.accent else colors.ink)) {
                                append(word.code)
                            }
                        }
                    }
                    Text(
                        text = text,
                        fontFamily = snippetLine.family,
                        fontSize = fontSizeSp.sp,
                        lineHeight = (fontSizeSp * 1.85f).sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
