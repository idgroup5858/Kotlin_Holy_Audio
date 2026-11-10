package com.example.kotlin_holy.ui.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.runtime.remember
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kotlin_holy.ui.theme.HolyTheme

/* Veb nusxadagi Tailwind chegaralari: sm — 640px, xl — 1280px, BOOK_GRID — 75rem (1200px) */
internal val SM = 640.dp
internal val BOOK_GRID_3 = 1200.dp
internal val XL = 1280.dp

/**
 * Jadval uslubidagi ro'yxatning (LIST_BOX) bitta qatori — qatorlar LazyColumn'da alohida
 * chiziladi: birinchisida yuqori burchaklar, oxirgisida pastki burchaklar yumaloq, hoshiya
 * esa butun qutini o'raydi (qatorlar orasida chiziq yo'q).
 */
@Composable
internal fun Modifier.listBoxRow(index: Int, count: Int): Modifier {
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

/**
 * To'rning bitta qatori: kataklar teng kenglikda va balandlikda (CSS grid kabi).
 * Bitta ustunda katak oddiygina to'liq kenglikda chiziladi. [cell] ga ikkinchi qiymat —
 * katak balandligi qator bo'yicha cho'zilganmi (flex-1 / mt-auto uchun).
 */
@Composable
internal fun <T> GridRow(
    cells: List<T>,
    columns: Int,
    gap: Dp,
    modifier: Modifier = Modifier,
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
 * flex flex-wrap items-center gap-2: bolalar qatorga sig'guncha yonma-yon, keyin keyingi qatorga.
 * [pushLast] — oxirgi bola ml-auto: o'z qatorining o'ng chetiga suriladi.
 */
@Composable
internal fun WrapRow(
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    pushLast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val policy = remember(spacing, pushLast) { WrapRowPolicy(spacing, pushLast) }
    Layout(content, modifier, policy)
}

/* Bolalarni qatorlarga bo'ladi: har qator — bolalar indekslari */
private fun breakLines(widths: List<Int>, gap: Int, maxWidth: Int): List<List<Int>> {
    val lines = mutableListOf<MutableList<Int>>()
    var lineWidth = 0
    widths.forEachIndexed { index, width ->
        if (lines.isEmpty() || lineWidth + gap + width > maxWidth) {
            lines += mutableListOf(index)
            lineWidth = width
        } else {
            lines.last() += index
            lineWidth += gap + width
        }
    }
    return lines
}

private class WrapRowPolicy(private val spacing: Dp, private val pushLast: Boolean) : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val gap = spacing.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val lines = breakLines(placeables.map { it.width }, gap, maxWidth)

        val lineHeights = lines.map { line -> line.maxOf { placeables[it].height } }
        val contentWidth = lines.maxOfOrNull { line -> line.sumOf { placeables[it].width } + gap * (line.size - 1) } ?: 0
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else contentWidth
        val height = (lineHeights.sum() + gap * (lines.size - 1).coerceAtLeast(0))
            .coerceIn(constraints.minHeight, if (constraints.hasBoundedHeight) constraints.maxHeight else Constraints.Infinity)

        return layout(width, height) {
            var y = 0
            lines.forEachIndexed { lineIndex, line ->
                var x = 0
                val lineHeight = lineHeights[lineIndex]
                line.forEach { index ->
                    val placeable = placeables[index]
                    val left = if (pushLast && index == placeables.lastIndex) width - placeable.width else x
                    placeable.placeRelative(left, y + (lineHeight - placeable.height) / 2)
                    x += placeable.width + gap
                }
                y += lineHeight + gap
            }
        }
    }

    /* Teng balandlikdagi to'r (IntrinsicSize.Min) uchun — aynan measure'dagi kabi qatorlarga bo'lib hisoblanadi */
    private fun IntrinsicMeasureScope.wrappedHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int {
        val gap = spacing.roundToPx()
        val maxWidth = if (width == Constraints.Infinity) Int.MAX_VALUE else width
        val widths = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity).coerceAtMost(maxWidth) }
        val heights = measurables.mapIndexed { index, measurable -> measurable.minIntrinsicHeight(widths[index]) }
        val lines = breakLines(widths, gap, maxWidth)
        return lines.sumOf { line -> line.maxOf { heights[it] } } + gap * (lines.size - 1).coerceAtLeast(0)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        wrappedHeight(measurables, width)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(measurables: List<IntrinsicMeasurable>, width: Int): Int =
        wrappedHeight(measurables, width)

    override fun IntrinsicMeasureScope.minIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int =
        measurables.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(measurables: List<IntrinsicMeasurable>, height: Int): Int {
        val gap = spacing.roundToPx()
        return measurables.sumOf { it.maxIntrinsicWidth(height) } + gap * (measurables.size - 1).coerceAtLeast(0)
    }
}

/* ---------------- Belgilar (veb nusxadagi inline SVG) ---------------- */

private fun strokeIcon(name: String, strokeWidth: Float, vararg paths: String): ImageVector =
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

internal object LibraryIcons {
    /** Matn sahifasi (Muqaddima, Atamalar lug'ati) */
    val Document = strokeIcon(
        "document", 1.8f,
        "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z",
        "M14 3v5h5M9 13h6M9 17h4",
    )

    /** «Davom etish ›» tugmasidagi qalin o'q */
    val ChevronRightBold = strokeIcon("chevron-right-bold", 2.5f, "M9 18l6-6-6-6")
}
