package com.example.kotlin_holy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.PageSummary
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.hex
import java.util.Calendar

/** Betda boshlanadigan suralar va davom etayotgan sura */
data class PageSurahs(val starting: List<Surah>, val current: Surah?)

/** Har bir betda qaysi sura boshlanadi yoki davom etadi — surah.startPage'dan */
fun surahsByPage(surahs: List<Surah>): Map<Int, PageSurahs> {
    val sorted = surahs.filter { it.startPage > 0 }.sortedBy { it.number }
    val result = HashMap<Int, PageSurahs>()
    var current: Surah? = null
    var cursor = 0
    for (page in 1..604) {
        val starting = mutableListOf<Surah>()
        while (cursor < sorted.size && sorted[cursor].startPage <= page) {
            if (sorted[cursor].startPage == page) starting += sorted[cursor]
            current = sorted[cursor]
            cursor++
        }
        result[page] = PageSurahs(starting, current)
    }
    return result
}

/* Bir kunda belgilangan betlar bir xil rangda: kunlar xronologik tartibda navbatdagi rangni oladi
   (birinchi kun — yashil), 10 kundan keyin ranglar qaytadan boshlanadi */
private val DAY_COLORS = listOf(
    "#10b981", "#0ea5e9", "#8b5cf6", "#f59e0b", "#f43f5e",
    "#6366f1", "#f97316", "#84cc16", "#d946ef", "#ec4899",
).map(::hex)

private fun dayOf(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${c.get(Calendar.YEAR)}-${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
}

/** Har bir belgilash kuni → rang (eng eski kun birinchi rangni oladi) */
fun dayColors(xatm: Map<Int, Long>): Map<String, Color> =
    xatm.values.sorted().map(::dayOf).distinct()
        .mapIndexed { index, day -> day to DAY_COLORS[index % DAY_COLORS.size] }
        .toMap()

fun dayColorOf(readAt: Long?, colors: Map<String, Color>): Color? = readAt?.let { colors[dayOf(it)] }

/** Betda boshlanadigan sura(lar) ajralib turadi, qolgan betlarda davom etayotgan sura xira */
@Composable
private fun SurahLabel(info: PageSurahs?) {
    val colors = HolyTheme.colors
    val current = info?.current ?: return
    if (info.starting.isNotEmpty()) {
        val first = info.starting.first()
        val rest = info.starting.size - 1
        Text(
            "${first.number}. ${first.nameUz}${if (rest > 0) " +$rest" else ""}",
            color = colors.accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(HolyTheme.shape)
                .background(colors.accent.copy(alpha = 0.10f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    } else {
        Text(
            current.nameUz,
            color = colors.inkSoft,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TranslationDot(has: Boolean) {
    val colors = HolyTheme.colors
    Box(Modifier.size(6.dp).clip(CircleShape).background(if (has) colors.accent else colors.line))
}

/**
 * Sahifalar katak yoki ro'yxat ko'rinishida — Sahifalar bo'limi, sura va juz betlari uchun
 * umumiy. LazyColumn ichida qatorlar bo'lib chiziladi: [rowsOf] bir qatorga nechta katak.
 */
@Composable
fun PageTileGrid(
    pages: List<PageSummary>,
    byPage: Map<Int, PageSurahs>,
    xatm: Map<Int, Long>,
    dayColors: Map<String, Color>,
    onOpen: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    columns: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        pages.forEach { page ->
            PageTile(page, byPage[page.number], xatm[page.number], dayColorOf(xatm[page.number], dayColors), onOpen, onToggle, Modifier.weight(1f))
        }
        repeat(columns - pages.size) { Box(Modifier.weight(1f)) }
    }
}

/** Telefon kengligiga qarab katak ustunlari: veb nusxadagi grid-cols-3 sm:5 lg:7 xl:8 2xl:10 */
fun pageGridColumns(width: Dp): Int = when {
    width >= 1536.dp -> 10
    width >= 1280.dp -> 8
    width >= 1024.dp -> 7
    width >= 640.dp -> 5
    else -> 3
}

@Composable
private fun PageTile(
    page: PageSummary,
    info: PageSurahs?,
    readAt: Long?,
    dayColor: Color?,
    onOpen: (Int) -> Unit,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val starts = info?.starting?.isNotEmpty() == true
    HolyCard(
        modifier = modifier,
        borderColor = when {
            dayColor != null -> dayColor.copy(alpha = 0.7f)
            starts -> colors.accent.copy(alpha = 0.45f)
            else -> null
        },
        background = when {
            dayColor != null -> mixOver(colors.surface, dayColor, 0.07f)
            starts -> mixOver(colors.surface, colors.accent, 0.05f)
            else -> null
        },
        onClick = { onOpen(page.number) },
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("${page.number}", color = colors.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("${page.ayahCount} oyat", color = colors.inkFaint, fontSize = 11.sp)
                SurahLabel(info)
                if (readAt != null) {
                    Text(
                        formatReadAt(readAt),
                        color = dayColor ?: colors.read,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                TranslationDot(page.hasTranslation)
            }
            ReadToggle(
                done = readAt != null,
                onToggle = { onToggle(page.number) },
                color = dayColor,
                size = 24.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                contentDescription = if (readAt != null) "${page.number}-sahifa belgisini olib tashlash" else "${page.number}-sahifani oʻqildi deb belgilash",
            )
            if (starts) {
                Text(
                    "۞",
                    color = colors.accent,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 10.dp),
                )
            }
        }
    }
}

/** Jadval uslubidagi bitta qator: belgi · bet raqami · sura · o'qilgan vaqt · oyatlar soni */
@Composable
fun PageListRow(
    page: PageSummary,
    index: Int,
    info: PageSurahs?,
    readAt: Long?,
    dayColor: Color?,
    wide: Boolean,
    onOpen: (Int) -> Unit,
    onToggle: (Int) -> Unit,
) {
    val colors = HolyTheme.colors
    val starts = info?.starting?.isNotEmpty() == true
    val background = when {
        dayColor != null -> mixOver(rowBackground(index), dayColor, 0.08f)
        starts -> mixOver(rowBackground(index), colors.accent, 0.05f)
        else -> rowBackground(index)
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(background)
            .then(
                if (dayColor != null) {
                    Modifier.drawBehind { drawRect(dayColor, Offset.Zero, Size(3.dp.toPx(), size.height)) }
                } else {
                    Modifier
                },
            )
            .clickable { onOpen(page.number) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) {
            ReadToggle(done = readAt != null, onToggle = { onToggle(page.number) }, color = dayColor, size = 24.dp)
        }
        Text("${page.number}", color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(48.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (starts) Text("۞", color = colors.accent, fontSize = 15.sp)
            SurahLabel(info)
        }
        if (wide) {
            Box(Modifier.width(120.dp)) {
                if (readAt != null) {
                    Text(formatReadAt(readAt), color = dayColor ?: colors.read, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
        Text(
            "${page.ayahCount} oyat",
            color = colors.inkFaint,
            fontSize = 12.sp,
            textAlign = TextAlign.End,
            modifier = if (wide) Modifier.width(72.dp) else Modifier,
        )
        if (wide) {
            Box(Modifier.width(16.dp), contentAlignment = Alignment.Center) { TranslationDot(page.hasTranslation) }
        }
    }
}

/** Kataklar yuklanayotganda */
@Composable
fun PageTilesSkeleton(view: String, count: Int = 14) {
    if (view == "list") {
        ListSkeleton(rows = minOf(count, 12), rowHeight = 44.dp)
        return
    }
    BoxWithConstraints {
        val columns = pageGridColumns(maxWidth)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            (0 until count).chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { Skeleton(Modifier.weight(1f).height(74.dp)) }
                    repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/**
 * LazyColumn ichida LIST_BOX (border-line rounded-md) qobig'ini qatorlarga bo'lib chizish:
 * har qator yon chiziqlarga ega, birinchisi tepa, oxirgisi past chiziq va burchaklarga.
 */
@Composable
fun Modifier.listBoxSegment(first: Boolean, last: Boolean): Modifier {
    val line = HolyTheme.colors.line
    val radius = HolyTheme.radius
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    return this
        .clip(shape)
        .drawWithContentOutline(line, radius, first, last)
}

private fun Modifier.drawWithContentOutline(color: Color, radius: Dp, first: Boolean, last: Boolean): Modifier =
    this.then(
        Modifier.drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            val r = radius.toPx()
            val half = stroke / 2
            val path = androidx.compose.ui.graphics.Path().apply {
                /* chap tomoni pastdan tepaga, tepasi, o'ng tomoni tepadan pastga, pasti */
                moveTo(half, if (last) size.height - r else size.height)
                lineTo(half, if (first) r else 0f)
                if (first) {
                    quadraticTo(half, half, r, half)
                    lineTo(size.width - r, half)
                    quadraticTo(size.width - half, half, size.width - half, r)
                } else {
                    moveTo(size.width - half, 0f)
                }
                lineTo(size.width - half, if (last) size.height - r else size.height)
                if (last) {
                    quadraticTo(size.width - half, size.height - half, size.width - r, size.height - half)
                    lineTo(r, size.height - half)
                    quadraticTo(half, size.height - half, half, size.height - r)
                }
            }
            drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(stroke))
        },
    )

/** Fon ustiga shaffof rangni qo'yish: bg-emerald-500/[0.07] kabi */
fun mixOver(base: Color, overlay: Color, alpha: Float): Color = Color(
    red = base.red * (1 - alpha) + overlay.red * alpha,
    green = base.green * (1 - alpha) + overlay.green * alpha,
    blue = base.blue * (1 - alpha) + overlay.blue * alpha,
    alpha = 1f,
)
