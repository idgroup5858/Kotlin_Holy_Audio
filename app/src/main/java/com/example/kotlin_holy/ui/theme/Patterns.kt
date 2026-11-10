package com.example.kotlin_holy.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.PathParser
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Orqa fon naqshlari — veb nusxadagi appearance.js dagi PATTERNS ning Compose'dagi chizilishi.
 * SVG naqshlar aynan o'sha yo'l (path) ma'lumotlaridan kataklarga chiziladi va takrorlanadi;
 * gradient naqshlar (chiziqlar, katak, halqalar, nur) to'g'ridan-to'g'ri chiziladi.
 * O'lchamlar CSS px da — Androidda 1 px = 1 dp, naqsh o'lchami (--pattern-scale) bilan kattalashadi.
 */

/** Naqsh rangi: matn rangining kuch foizi yoki (asosiy rangda) biroz kuchliroq asosiy rang */
fun patternColor(appearance: Appearance, colors: HolyColors): Color {
    val percent = appearance.strengthPercent
    return if (appearance.tint) {
        colors.accent.copy(alpha = (percent * 1.6f).roundToInt() / 100f)
    } else {
        colors.ink.copy(alpha = percent / 100f)
    }
}

/** Sahifa orqasidagi qatlam: naqsh va (yoqilgan bo'lsa) rangli yog'du */
@Composable
fun AppearanceBackdrop(
    appearance: Appearance,
    colors: HolyColors,
    modifier: Modifier = Modifier,
    pattern: String = appearance.pattern,
    showGlow: Boolean = appearance.glow,
) {
    val color = patternColor(appearance, colors)
    val scale = appearance.patternScale
    val glowAccent = colors.accent
    val glowPercent = appearance.strengthPercent
    Spacer(
        modifier.drawWithCache {
            val drawPattern = buildPattern(pattern, color, density, scale, size.width, size.height)
            val drawGlow: (DrawScope.() -> Unit)? =
                if (showGlow) buildGlow(glowAccent, glowPercent, density, size.width, size.height) else null
            onDrawBehind {
                drawGlow?.invoke(this)
                drawPattern?.invoke(this)
            }
        },
    )
}

/* ---------------- yordamchi shakllar (appearance.js bilan bir xil) ---------------- */

private fun circlePath(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}A$r $r 0 1 0 ${cx + r} ${cy}A$r $r 0 1 0 ${cx - r} ${cy}Z"

/* Yo'l matnida 1.2E-16 kabi yozuv bo'lmasligi uchun — PathParser uni tushunmaydi */
private fun n(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
private fun n(value: Float): String = n(value.toDouble())

/** {n/k} yulduz ko'pburchagi — islomiy naqshlardagi kabi bir-birini kesib o'tgan qirralar */
private fun starPolygon(cx: Float, cy: Float, radius: Float, points: Int, step: Int): String {
    val vertices = (0 until points).map { index ->
        val angle = index * step * 2 * PI / points
        "${n(cx + radius * cos(angle))} ${n(cy + radius * sin(angle))}"
    }
    return "M" + vertices.joinToString("L") + "Z"
}

/** Hilol: tashqi doiradan siljigan ichki doira kesib olingan */
private fun crescentPath(cx: Float, cy: Float, radius: Float, offset: Float, angle: Float): android.graphics.Path {
    val half = offset / 2
    val h = sqrt(radius * radius - half * half)
    val x = cx + half
    val path = PathParser.createPathFromPathData(
        "M${n(x)} ${n(cy - h)}A$radius $radius 0 1 0 ${n(x)} ${n(cy + h)}A$radius $radius 0 0 1 ${n(x)} ${n(cy - h)}Z",
    )
    path.transform(Matrix().apply { setRotate(angle, cx, cy) })
    return path
}

/** To'rt qirrali yulduzcha (uchqun) */
private fun sparkle(x: Float, y: Float, r: Float): String {
    val k = r * 0.2f
    return "M$x ${y - r}Q${x + k} ${y - k} ${x + r} ${y}Q${x + k} ${y + k} $x ${y + r}" +
        "Q${x - k} ${y + k} ${x - r} ${y}Q${x - k} ${y - k} $x ${y - r}Z"
}

/** Burchakdagi shakl to'rt burchakda ham chiziladi — qo'shni kataklar bilan birga to'liq bo'ladi */
private fun corners(w: Float, h: Float, shape: (Float, Float) -> String) =
    listOf(0f to 0f, w to 0f, 0f to h, w to h).joinToString("") { (x, y) -> shape(x, y) }

/** SVG katak: chiziqlar (stroke) va to'ldirilgan (fill) shakllar */
private class SvgTile(
    val width: Float,
    val height: Float,
    val stroke: String = "",
    val fill: String = "",
    val extraFill: List<android.graphics.Path> = emptyList(),
)

private fun svgTile(id: String): SvgTile? = when (id) {
    "zigzag" -> SvgTile(24f, 14f, "M0 12L6 2L12 12L18 2L24 12")
    "maze" -> SvgTile(24f, 24f, "M0 22H24M3 22V4H21V18H9V10H15V14")
    "diamonds" -> SvgTile(28f, 28f, "M14 7L21 14L14 21L7 14Z", corners(28f, 28f) { x, y -> circlePath(x, y, 1.6f) })
    "triangles" -> SvgTile(30f, 51.96f, "M0 0H30M0 25.98H30M0 51.96H30M0 0L30 51.96M30 0L0 51.96")
    "hexagons" -> SvgTile(48f, 27.71f, "M40 13.86L32 27.71H16L8 13.86L16 0H32ZM0 13.86H8M40 13.86H48")
    "bricks" -> SvgTile(48f, 24f, "M0 0H48M0 12H48M0 24H48M12 0V12M36 0V12M0 12V24M24 12V24M48 12V24")
    "scales" -> SvgTile(
        40f, 40f,
        "M0 21A20 20 0 0 1 40 21M8 21A12 12 0 0 1 32 21M0 21A20 20 0 0 1 20 41M20 41A20 20 0 0 1 40 21" +
            "M0 29A12 12 0 0 1 12 41M28 41A12 12 0 0 1 40 29",
    )
    "flowers" -> SvgTile(40f, 40f, circlePath(20f, 20f, 20f) + corners(40f, 40f) { x, y -> circlePath(x, y, 20f) })
    "stars" -> SvgTile(
        60f, 60f,
        "M30 9L51 30L30 51L9 30ZM15.15 15.15H44.85V44.85H15.15Z" + circlePath(30f, 30f, 6f) +
            "M30 0V9M30 51V60M0 30H9M51 30H60",
    )
    "star8" -> SvgTile(
        40f, 40f,
        starPolygon(20f, 20f, 14f, 8, 3) +
            corners(40f, 40f) { x, y -> "M$x ${y - 4}L${x + 4} ${y}L$x ${y + 4}L${x - 4} ${y}Z" },
    )
    "shamsa" -> SvgTile(
        48f, 48f,
        starPolygon(24f, 24f, 16f, 12, 5) + circlePath(24f, 24f, 19f) +
            corners(48f, 48f) { x, y -> circlePath(x, y, 3f) },
    )
    "octagons" -> SvgTile(
        40f, 40f,
        "M11.72 0H28.28L40 11.72V28.28L28.28 40H11.72L0 28.28V11.72Z" +
            "M15.86 10H24.14L30 15.86V24.14L24.14 30H15.86L10 24.14V15.86Z",
    )
    "lattice" -> SvgTile(
        24f, 24f,
        "M0 0L24 24M24 0L0 24" + circlePath(12f, 12f, 3.5f) + corners(24f, 24f) { x, y -> circlePath(x, y, 3.5f) },
    )
    "arches" -> SvgTile(
        36f, 52f,
        "M6 52V28C6 18 12 11 18 4C24 11 30 18 30 28V52M11 52V29C11 22 14 17 18 12C22 17 25 22 25 29V52",
    )
    "crescents" -> SvgTile(
        80f, 80f,
        fill = circlePath(58f, 20f, 1.3f) + circlePath(16f, 62f, 1.1f) + circlePath(40f, 48f, 0.9f),
        extraFill = listOf(crescentPath(24f, 26f, 9f, 5f, -30f), crescentPath(60f, 60f, 6f, 3.4f, -30f)),
    )
    "waves" -> SvgTile(80f, 24f, "M0 12C10 4 30 4 40 12S70 20 80 12")
    "dunes" -> SvgTile(
        120f, 42f,
        "M0 8C20 2 40 2 60 8S100 14 120 8M0 22C20 12 40 12 60 22S100 32 120 22M0 35C20 32 40 32 60 35S100 38 120 35",
    )
    "sky" -> SvgTile(
        140f, 140f,
        fill = listOf(
            Triple(18f, 22f, 6f), Triple(96f, 16f, 3.5f), Triple(122f, 64f, 5f), Triple(60f, 52f, 2.6f),
            Triple(30f, 98f, 4f), Triple(104f, 112f, 2.8f), Triple(74f, 128f, 1.8f),
        ).joinToString("") { (x, y, r) -> sparkle(x, y, r) } +
            listOf(
                Triple(44f, 20f, 1f), Triple(130f, 30f, 0.9f), Triple(84f, 82f, 1.2f),
                Triple(12f, 64f, 0.8f), Triple(56f, 118f, 0.9f), Triple(118f, 92f, 1f),
            ).joinToString("") { (x, y, r) -> circlePath(x, y, r) },
    )
    else -> null
}

/** Kataklik naqsh: bitmap katak yasaladi va butun maydonga takrorlanadi */
private fun tileBrush(width: Float, height: Float, unit: Float, draw: (android.graphics.Canvas, Float, Float) -> Unit): ShaderBrush {
    val bw = ceil(width * unit).toInt().coerceAtLeast(1)
    val bh = ceil(height * unit).toInt().coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    /* Katak butun piksel bo'lishi uchun o'lcham biroz moslanadi — chok ko'rinmaydi */
    draw(canvas, bw / width, bh / height)
    return ShaderBrush(BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT))
}

private fun opaquePaint(color: Color, style: Paint.Style, strokeWidth: Float = 0f) = Paint().apply {
    isAntiAlias = true
    this.color = color.copy(alpha = 1f).toArgb()
    this.style = style
    this.strokeWidth = strokeWidth
    strokeCap = Paint.Cap.BUTT
}

/**
 * Naqshni chizuvchi funksiya. [unit] — 1 CSS px ning px dagi qiymati (zichlik),
 * [scale] — naqsh o'lchami (Kichik / O'rta / Katta).
 */
private fun buildPattern(
    id: String,
    color: Color,
    unit: Float,
    scale: Float,
    width: Float,
    height: Float,
): (DrawScope.() -> Unit)? {
    if (id == "none" || width <= 0f || height <= 0f) return null
    val u = unit * scale
    val px = unit
    val alpha = color.alpha
    val solid = color.copy(alpha = 1f)

    svgTile(id)?.let { tile ->
        val brush = tileBrush(tile.width, tile.height, u) { canvas, sx, sy ->
            canvas.scale(sx, sy)
            if (tile.stroke.isNotEmpty()) {
                canvas.drawPath(
                    PathParser.createPathFromPathData(tile.stroke),
                    opaquePaint(color, Paint.Style.STROKE, 1.1f),
                )
            }
            val fillPaint = opaquePaint(color, Paint.Style.FILL)
            if (tile.fill.isNotEmpty()) canvas.drawPath(PathParser.createPathFromPathData(tile.fill), fillPaint)
            tile.extraFill.forEach { canvas.drawPath(it, fillPaint) }
        }
        return { drawRect(brush, alpha = alpha) }
    }

    return when (id) {
        /* repeating-linear-gradient(135deg, rang 0 1px, shaffof 1px 12px) */
        "lines" -> diagonal(width, height, 12 * u, px, solid, alpha, rising = true)
        "stripes" -> diagonal(width, height, 24 * u, 8 * u, solid, alpha * 0.6f, rising = true)
        "cross" -> {
            val a = diagonal(width, height, 16 * u, px, solid, alpha, rising = true)
            val b = diagonal(width, height, 16 * u, px, solid, alpha, rising = false)
            ({ a(); b() })
        }
        "columns" -> straight(width, height, 22 * u, px, solid, alpha, vertical = true, horizontal = false)
        "notebook" -> straight(width, height, 28 * u, px, solid, alpha, vertical = false, horizontal = true)
        "grid" -> straight(width, height, 32 * u, px, solid, alpha, vertical = true, horizontal = true)
        "graph" -> {
            val minor = straight(width, height, 16 * u, px, solid, alpha * 0.45f, vertical = true, horizontal = true)
            val major = straight(width, height, 80 * u, px, solid, alpha, vertical = true, horizontal = true)
            ({ minor(); major() })
        }
        "plaid" -> {
            /* har 44px da: 4px qalin tasma va 12px dagi 1px chiziq — ikkala yo'nalishda */
            val period = 44 * u
            val bands = Path()
            val lines = Path()
            var p = 0f
            while (p < width + period) {
                bands.addRect(androidx.compose.ui.geometry.Rect(p, 0f, p + 4 * u, height))
                lines.addRect(androidx.compose.ui.geometry.Rect(p + 12 * u, 0f, p + 12 * u + px, height))
                p += period
            }
            p = 0f
            while (p < height + period) {
                bands.addRect(androidx.compose.ui.geometry.Rect(0f, p, width, p + 4 * u))
                lines.addRect(androidx.compose.ui.geometry.Rect(0f, p + 12 * u, width, p + 12 * u + px))
                p += period
            }
            ({
                drawPath(bands, solid, alpha = alpha * 0.7f)
                drawPath(lines, solid, alpha = alpha * 0.7f)
            })
        }
        "dots" -> {
            val brush = tileBrush(20f, 20f, u) { canvas, sx, sy ->
                canvas.drawCircle(10f * sx, 10f * sy, 1.6f * px, opaquePaint(color, Paint.Style.FILL))
            }
            ({ drawRect(brush, alpha = alpha) })
        }
        "rings" -> {
            val brush = tileBrush(26f, 26f, u) { canvas, sx, sy ->
                canvas.drawCircle(13f * sx, 13f * sy, 3f * u + px / 2, opaquePaint(color, Paint.Style.STROKE, px))
            }
            ({ drawRect(brush, alpha = alpha) })
        }
        "checker" -> {
            val brush = tileBrush(40f, 40f, u) { canvas, sx, sy ->
                val paint = opaquePaint(color, Paint.Style.FILL)
                canvas.drawRect(20f * sx, 0f, 40f * sx, 20f * sy, paint)
                canvas.drawRect(0f, 20f * sy, 20f * sx, 40f * sy, paint)
            }
            ({ drawRect(brush, alpha = alpha * 0.55f) })
        }
        "ripples" -> {
            /* Markazdan (50% 40%) tarqalgan halqalar, chetga borib so'nadi */
            val center = Offset(width * 0.5f, height * 0.4f)
            val far = maxOf(
                hypot(center.x, center.y), hypot(width - center.x, center.y),
                hypot(center.x, height - center.y), hypot(width - center.x, height - center.y),
            )
            val rings = Path()
            var r = 26 * u
            while (r < far) {
                rings.addOval(androidx.compose.ui.geometry.Rect(center, r))
                r += 26 * u
            }
            val brush = Brush.radialGradient(
                0f to color, 0.25f to color, 0.8f to color.copy(alpha = 0f),
                center = center, radius = far,
            )
            ({ drawPath(rings, brush, style = Stroke(px)) })
        }
        "rays" -> {
            /* Yuqoridan (50% -15%) tushayotgan nur, pastga borib so'nadi */
            val origin = Offset(width * 0.5f, -height * 0.15f)
            val length = hypot(width, height) * 2
            val step = 6f * scale
            val wedges = Path()
            var angle = 0f
            while (angle < 360f) {
                val a1 = Math.toRadians(angle.toDouble() - 90)
                val a2 = Math.toRadians(angle.toDouble() + 1 - 90)
                wedges.moveTo(origin.x, origin.y)
                wedges.lineTo(origin.x + (length * cos(a1)).toFloat(), origin.y + (length * sin(a1)).toFloat())
                wedges.lineTo(origin.x + (length * cos(a2)).toFloat(), origin.y + (length * sin(a2)).toFloat())
                wedges.close()
                angle += step
            }
            val brush = Brush.verticalGradient(
                0f to color, 0.9f to color.copy(alpha = 0f),
                startY = 0f, endY = height,
            )
            ({ drawPath(wedges, brush) })
        }
        "paper" -> {
            /* Qog'oz donasi: feTurbulence o'rniga tasodifiy shovqin, chetlari takrorlanadi */
            val brush = tileBrush(180f, 180f, unit) { canvas, sx, sy ->
                val w = (180 * sx).toInt()
                val h = (180 * sy).toInt()
                val random = Random(7)
                val pixels = IntArray(w * h)
                val rgb = solid.toArgb() and 0x00FFFFFF
                for (i in pixels.indices) {
                    val noise = 0.5f + (random.nextFloat() + random.nextFloat() - 1f) * 0.32f
                    val a = (2.4f * noise - 0.9f).coerceIn(0f, 1f)
                    pixels[i] = ((a * 255).toInt() shl 24) or rgb
                }
                val noiseBitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888)
                canvas.drawBitmap(noiseBitmap, 0f, 0f, null)
            }
            ({ drawRect(brush, alpha = alpha) })
        }
        else -> null
    }
}

/** 45° li parallel chiziqlar: [period] — chiziqlar orasidagi tik masofa */
private fun diagonal(
    width: Float,
    height: Float,
    period: Float,
    thickness: Float,
    color: Color,
    alpha: Float,
    rising: Boolean,
): DrawScope.() -> Unit {
    val step = period * sqrt(2f)
    val path = Path()
    var c = -step
    while (c < width + height + step) {
        if (rising) {
            /* x + y = c */
            path.moveTo(c, 0f)
            path.lineTo(c - height, height)
        } else {
            /* x - y = c - h */
            path.moveTo(c - height, 0f)
            path.lineTo(c, height)
        }
        c += step
    }
    return { drawPath(path, color, alpha = alpha, style = Stroke(thickness, cap = StrokeCap.Butt)) }
}

/** Tik va/yoki yotiq chiziqlar har [period] da */
private fun straight(
    width: Float,
    height: Float,
    period: Float,
    thickness: Float,
    color: Color,
    alpha: Float,
    vertical: Boolean,
    horizontal: Boolean,
): DrawScope.() -> Unit {
    val path = Path()
    if (vertical) {
        var x = 0f
        while (x < width) {
            path.addRect(androidx.compose.ui.geometry.Rect(x, 0f, x + thickness, height))
            x += period
        }
    }
    if (horizontal) {
        var y = 0f
        while (y < height) {
            path.addRect(androidx.compose.ui.geometry.Rect(0f, y, width, y + thickness))
            y += period
        }
    }
    return { drawPath(path, color, alpha = alpha) }
}

/** Asosiy rangdagi yumshoq yorug'lik — chap yuqori va o'ng pastki burchakdan */
private fun buildGlow(accent: Color, percent: Int, unit: Float, width: Float, height: Float): DrawScope.() -> Unit {
    fun ellipse(cx: Float, cy: Float, rx: Float, ry: Float, amount: Float): ShaderBrush {
        val gradient = RadialGradient(
            cx, cy, rx,
            intArrayOf(accent.copy(alpha = amount).toArgb(), accent.copy(alpha = 0f).toArgb()),
            floatArrayOf(0f, 0.7f),
            Shader.TileMode.CLAMP,
        )
        gradient.setLocalMatrix(Matrix().apply { setScale(1f, ry / rx, cx, cy) })
        return ShaderBrush(gradient)
    }
    val rem = 16 * unit
    val first = ellipse(0f, 0f, 60 * rem, 42 * rem, (percent * 3) / 100f)
    val second = ellipse(width, height, 52 * rem, 40 * rem, (percent * 2) / 100f)
    return {
        drawRect(first)
        drawRect(second)
    }
}
