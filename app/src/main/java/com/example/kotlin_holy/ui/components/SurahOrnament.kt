package com.example.kotlin_holy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.theme.HolyFonts
import com.example.kotlin_holy.ui.theme.HolyTheme

/*
 * Bosma mushafdagi kabi naqshli sura sarlavhasi. Chizmaning o'lchamlari
 * veb nusxadagi SVG bilan bir xil: eni 1024, balandligi 80 birlik.
 */
private const val FRAME_W = 1024f
private const val FRAME_H = 80f

@Composable
fun SurahHeaderOrnament(
    surahNumber: Int,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val context = LocalContext.current
    val nameFamily = rememberSurahNameFamily()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(FRAME_W / FRAME_H),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(FRAME_W / FRAME_H)) {
            val scale = size.width / FRAME_W
            val accent = colors.accent
            val surface = colors.surface

            fun x(value: Float) = (value + 12f) * scale
            fun y(value: Float) = value * scale
            fun s(value: Float) = value * scale

            /* tashqi va ichki hoshiya */
            drawRoundRectFrame(
                left = x(2f), top = y(3f), width = s(996f), height = s(74f),
                radius = s(7f), color = accent, fillAlpha = 0.06f, strokeWidth = s(1.8f),
            )
            drawRoundRectFrame(
                left = x(9f), top = y(10f), width = s(982f), height = s(60f),
                radius = s(4f), color = accent, fillAlpha = 0f, strokeWidth = s(0.8f),
                strokeAlpha = 0.7f,
            )

            /* yon panellardagi romb naqsh */
            val tile = s(20f)
            var px = x(9f)
            while (px < x(991f)) {
                var py = y(10f)
                while (py < y(70f)) {
                    drawDiamond(px + tile / 2, py + tile / 2, tile * 0.38f, accent, s(0.9f), 0.4f)
                    drawDiamond(px + tile / 2, py + tile / 2, tile * 0.15f, accent, 0f, 0.22f, fill = true)
                    py += tile
                }
                px += tile
            }

            /* chetlardagi bargchalar */
            listOf(2f, 998f).forEach { cx ->
                drawLeaf(x(cx), y(40f), s(14f), s(10f), surface, accent, s(1.4f))
            }

            /* sakkiz yaproqli gullar */
            listOf(166f, 834f).forEach { cx ->
                drawRosette(x(cx), y(40f), s(23f), surface, accent, s(1.4f))
            }

            /* o'rtadagi medalyon */
            val cartouche = cartouchePath(::x, ::y)
            drawPath(cartouche, color = surface)
            drawPath(cartouche, color = accent, style = Stroke(width = s(1.8f)))
            listOf(318f, 682f).forEach { cx ->
                drawCircle(accent, radius = s(3.2f), center = Offset(x(cx), y(40f)))
            }
        }

        /* Nom xattotlik shriftida: "018 surah" → «سورة الكهف» */
        Text(
            text = "${surahNumber.toString().padStart(3, '0')} surah",
            fontFamily = nameFamily,
            color = colors.ink,
            fontSize = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun rememberSurahNameFamily() = run {
    val context = LocalContext.current
    androidx.compose.runtime.remember { HolyFonts.surahNames(context.assets) }
}

private fun DrawScope.drawRoundRectFrame(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
    color: androidx.compose.ui.graphics.Color,
    fillAlpha: Float,
    strokeWidth: Float,
    strokeAlpha: Float = 1f,
) {
    if (fillAlpha > 0f) {
        drawRoundRect(
            color = color.copy(alpha = fillAlpha),
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
    }
    drawRoundRect(
        color = color.copy(alpha = strokeAlpha),
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        style = Stroke(width = strokeWidth),
    )
}

private fun DrawScope.drawDiamond(
    cx: Float,
    cy: Float,
    r: Float,
    color: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
    alpha: Float,
    fill: Boolean = false,
) {
    val path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }
    if (fill) drawPath(path, color.copy(alpha = alpha))
    else drawPath(path, color.copy(alpha = alpha), style = Stroke(width = strokeWidth))
}

private fun DrawScope.drawLeaf(
    cx: Float,
    cy: Float,
    halfHeight: Float,
    halfWidth: Float,
    fill: androidx.compose.ui.graphics.Color,
    stroke: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
) {
    val path = Path().apply {
        moveTo(cx, cy - halfHeight)
        lineTo(cx + halfWidth, cy)
        lineTo(cx, cy + halfHeight)
        lineTo(cx - halfWidth, cy)
        close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = strokeWidth))
    drawDiamond(cx, cy, halfHeight / 2f, stroke, 0f, 1f, fill = true)
}

private fun DrawScope.drawRosette(
    cx: Float,
    cy: Float,
    radius: Float,
    fill: androidx.compose.ui.graphics.Color,
    accent: androidx.compose.ui.graphics.Color,
    strokeWidth: Float,
) {
    drawCircle(fill, radius = radius, center = Offset(cx, cy))
    drawCircle(accent, radius = radius, center = Offset(cx, cy), style = Stroke(width = strokeWidth))
    drawCircle(
        accent.copy(alpha = 0.6f),
        radius = radius * 0.82f,
        center = Offset(cx, cy),
        style = Stroke(width = strokeWidth * 0.45f),
    )
    repeat(8) { index ->
        translate(cx, cy) {
            rotate(index * 45f, Offset.Zero) {
                drawOval(
                    color = accent.copy(alpha = if (index % 2 == 0) 0.8f else 0.45f),
                    topLeft = Offset(-radius * 0.16f, -radius * 0.76f),
                    size = Size(radius * 0.32f, radius * 0.74f),
                )
            }
        }
    }
    drawCircle(fill, radius = radius * 0.15f, center = Offset(cx, cy))
    drawCircle(
        accent,
        radius = radius * 0.15f,
        center = Offset(cx, cy),
        style = Stroke(width = strokeWidth * 0.85f),
    )
}

private fun cartouchePath(x: (Float) -> Float, y: (Float) -> Float): Path = Path().apply {
    moveTo(x(318f), y(40f))
    cubicTo(x(346f), y(40f), x(356f), y(10f), x(388f), y(10f))
    lineTo(x(612f), y(10f))
    cubicTo(x(644f), y(10f), x(654f), y(40f), x(682f), y(40f))
    cubicTo(x(654f), y(40f), x(644f), y(70f), x(612f), y(70f))
    lineTo(x(388f), y(70f))
    cubicTo(x(356f), y(70f), x(346f), y(40f), x(318f), y(40f))
    close()
}

/**
 * Basmala 1-betdagi Fotihaning 1-oyati — o'sha sahifa shriftidagi glifdan
 * olinadi, shunda u boshqa satrlar bilan bir xil mushaf uslubida chiqadi.
 */
private val BASMALA_CODES = listOf("ﭑ", "ﭒ", "ﭓ", "ﭔ")

@Composable
fun BasmalaRow(fontSizeSp: Float, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val (family, _) = rememberMushafFont(1)

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = BASMALA_CODES.joinToString(" "),
            fontFamily = family,
            color = colors.ink,
            fontSize = fontSizeSp.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Mehribon va Rahmli Allohning nomi bilan boshlayman",
            color = colors.inkFaint,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
    }
}
