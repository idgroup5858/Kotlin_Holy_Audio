package com.example.kotlin_holy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cbrt
import kotlin.math.pow

/**
 * Mavzular (Sozlamalar → Mavzu) — veb nusxadagi src/lib/appearance.js ning aynan o'zi:
 * ranglar palitrasi, asosiy rang, orqa fon naqshi, kartochkalar uslubi va burchaklar.
 * Har bir mavzuning yorug' va qorong'i varianti bor — tepadagi almashtirgich odatdagidek ishlaydi.
 * id lar saqlangan tanlov bilan bog'liq — o'zgartirilmaydi.
 */

@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val inkSoft: Color,
    val inkFaint: Color,
    val accent: Color,
    val accentHover: Color,
    val accentSoft: Color,
)

/* Palitra tartibi — index.css dagi o'zgaruvchilar bilan bir xil:
   bg surface surface-2 line ink ink-soft ink-faint accent hover soft */
private fun palette(colors: String): Palette {
    val c = colors.trim().split(Regex("\\s+")).map(::hex)
    return Palette(c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9])
}

fun hex(value: String): Color = Color(("FF" + value.removePrefix("#")).toLong(16))

@Immutable
data class ThemeDef(val id: String, val name: String, val hint: String, val light: Palette, val dark: Palette)

val THEMES: List<ThemeDef> = listOf(
    ThemeDef(
        "default", "Feruza", "Standart",
        palette("#f6f6f7 #ffffff #f0f0f2 #e3e3e7 #17181b #55565e #9b9ca5 #157c85 #10646c #e4f2f3"),
        palette("#0f1012 #1c1d21 #26272c #2c2d34 #f2f2f4 #a0a1aa #6e6f79 #4ec3cc #6ad3db #14343a"),
    ),
    ThemeDef(
        "night", "Tungi koʻk", "Toʻq koʻk, siyohrang",
        palette("#f2f4fb #ffffff #e9edf8 #dbe1f0 #111834 #48527a #8a94b6 #4f46e5 #4338ca #e7e6fd"),
        palette("#0a0f1f #111a33 #18223f #222d4f #edf0fb #a7b1d2 #66729b #818cf8 #a5adfb #1f2452"),
    ),
    ThemeDef(
        "bordo", "Bordo", "Toʻq qizil, issiq",
        palette("#f8eef1 #ffffff #f4e6ea #ead5db #2b0d17 #6d3846 #a9808b #9f1239 #881337 #fbe4ea"),
        palette("#2a0b16 #3a1220 #47182a #5a2236 #fdf0f3 #e0b3c0 #a8768a #fb7185 #fda4af #561a2c"),
    ),
    ThemeDef(
        "emerald", "Zumrad", "Yashil, tinch",
        palette("#f1f8f4 #ffffff #e5f2eb #d3e7dc #0b2118 #3d5f50 #84a596 #047857 #065f46 #d9f2e6"),
        palette("#07130e #0e1f18 #142a21 #1c372c #eaf7f0 #9cc3b1 #5f8a76 #34d399 #6ee7b7 #0f3526"),
    ),
    ThemeDef(
        "mushaf", "Mushaf", "Sargʻish qogʻoz",
        palette("#f3ead7 #fbf6ea #eee2c8 #e0cfab #2a1f12 #5d4a30 #9a8564 #8a5a14 #714910 #f2e2bf"),
        palette("#16120c #211b12 #2b2318 #3a2f20 #f3e8d3 #c5b291 #88775b #e3b55e #ecc87f #3b2e15"),
    ),
    ThemeDef(
        "violet", "Binafsha", "Yumshoq binafsha",
        palette("#f6f3fd #ffffff #eee8fa #e1d8f4 #1a1129 #54457a #998eb8 #7c3aed #6d28d9 #ede5fd"),
        palette("#100b1b #1a1329 #231a36 #2e2346 #f4f0fb #b9acd6 #7a6c99 #a78bfa #c4b5fd #2b1f4a"),
    ),
    ThemeDef(
        "ocean", "Okean", "Moviy, salqin",
        palette("#f0f6fb #ffffff #e3edf6 #d1e0ee #0a1b2a #3d5872 #85a0b7 #0369a1 #075985 #dbedf8"),
        palette("#06111b #0c1b2a #122539 #1a3048 #e9f3fb #9cb9d3 #5e7e9b #38bdf8 #7dd3fc #0d2f48"),
    ),
    ThemeDef(
        "graphite", "Grafit", "Kulrang, oltin rang",
        palette("#f5f5f4 #ffffff #eeeeec #e2e1de #1c1917 #57534e #a29d97 #b45309 #92400e #fbeedb"),
        palette("#111111 #1b1b1b #252525 #2f2f2f #f2f2f2 #a8a8a8 #6c6c6c #f5a524 #f8bb55 #362a13"),
    ),
    ThemeDef(
        "rose", "Atirgul", "Pushti",
        palette("#fdf4f8 #ffffff #f9e8f0 #f1d7e4 #2a0f1c #6f3a53 #ae8499 #be185d #9d174d #fce4f0"),
        palette("#170a11 #22101a #2d1623 #3b1d2e #fbeef5 #d9a8c1 #9b6a82 #f472b6 #f9a8d4 #3f1631"),
    ),
    ThemeDef(
        "contrast", "Kontrast", "Sof oq / sof qora",
        palette("#ffffff #ffffff #f4f4f5 #d4d4d8 #000000 #27272a #71717a #0f766e #115e59 #dcf3f1"),
        palette("#000000 #0a0a0a #141414 #262626 #ffffff #c4c4c8 #7c7c84 #2dd4bf #5eead4 #0b2c2a"),
    ),
)

/** Asosiy rang mavzudagidan boshqa bo'lishi mumkin: [yorug' rejimda, qorong'i rejimda] */
@Immutable
data class AccentDef(val id: String, val name: String, val light: Color? = null, val dark: Color? = null)

val ACCENTS: List<AccentDef> = listOf(
    AccentDef("theme", "Mavzudagi"),
    AccentDef("bordo", "Bordo", hex("#9f1239"), hex("#fb7185")),
    AccentDef("red", "Qizil", hex("#b91c1c"), hex("#f87171")),
    AccentDef("pink", "Pushti", hex("#be185d"), hex("#f472b6")),
    AccentDef("purple", "Binafsha", hex("#7e22ce"), hex("#c084fc")),
    AccentDef("indigo", "Siyohrang", hex("#4338ca"), hex("#818cf8")),
    AccentDef("blue", "Koʻk", hex("#1d4ed8"), hex("#60a5fa")),
    AccentDef("sky", "Osmon", hex("#0369a1"), hex("#38bdf8")),
    AccentDef("teal", "Feruza", hex("#157c85"), hex("#4ec3cc")),
    AccentDef("emerald", "Zumrad", hex("#047857"), hex("#34d399")),
    AccentDef("green", "Yashil", hex("#15803d"), hex("#4ade80")),
    AccentDef("olive", "Zaytun", hex("#4d7c0f"), hex("#a3e635")),
    AccentDef("gold", "Oltin", hex("#a16207"), hex("#facc15")),
    AccentDef("orange", "Toʻq sariq", hex("#c2410c"), hex("#fb923c")),
    AccentDef("brown", "Jigarrang", hex("#7c4a2d"), hex("#d6a27a")),
    AccentDef("slate", "Grafit", hex("#334155"), hex("#94a3b8")),
)

/* ---------------- Orqa fon naqshlari ---------------- */

@Immutable
data class PatternGroup(val id: String, val title: String)

val PATTERN_GROUPS = listOf(
    PatternGroup("simple", "Oddiy"),
    PatternGroup("geometric", "Geometrik"),
    PatternGroup("islamic", "Islomiy naqshlar"),
    PatternGroup("natural", "Tabiiy"),
)

@Immutable
data class PatternDef(val id: String, val group: String, val name: String)

/** Chizilishi Patterns.kt da — rangi matn (yoki asosiy) rangidan, shuning uchun har mavzuga mos */
val PATTERNS: List<PatternDef> = listOf(
    PatternDef("none", "simple", "Yoʻq"),
    PatternDef("lines", "simple", "Chiziqlar"),
    PatternDef("stripes", "simple", "Yoʻl-yoʻl"),
    PatternDef("columns", "simple", "Ustunlar"),
    PatternDef("notebook", "simple", "Daftar"),
    PatternDef("cross", "simple", "Toʻr"),
    PatternDef("grid", "simple", "Katak"),
    PatternDef("graph", "simple", "Chizma qogʻozi"),
    PatternDef("plaid", "simple", "Mato katagi"),
    PatternDef("dots", "simple", "Nuqtalar"),
    PatternDef("rings", "simple", "Doirachalar"),
    PatternDef("checker", "simple", "Shaxmat"),
    PatternDef("zigzag", "geometric", "Zigzag"),
    PatternDef("maze", "geometric", "Labirint"),
    PatternDef("diamonds", "geometric", "Romblar"),
    PatternDef("triangles", "geometric", "Uchburchaklar"),
    PatternDef("hexagons", "geometric", "Asalari uyasi"),
    PatternDef("bricks", "geometric", "Gʻisht"),
    PatternDef("scales", "geometric", "Tangachalar"),
    PatternDef("flowers", "geometric", "Gullar"),
    PatternDef("stars", "islamic", "Rubʼ al-hizb"),
    PatternDef("star8", "islamic", "Sakkiz yulduz"),
    PatternDef("shamsa", "islamic", "Shamsa"),
    PatternDef("octagons", "islamic", "Sakkizburchak"),
    PatternDef("lattice", "islamic", "Panjara"),
    PatternDef("arches", "islamic", "Mehrob"),
    PatternDef("crescents", "islamic", "Hilol"),
    PatternDef("waves", "natural", "Toʻlqin"),
    PatternDef("dunes", "natural", "Qum barxanlari"),
    PatternDef("ripples", "natural", "Halqalar"),
    PatternDef("rays", "natural", "Nur"),
    PatternDef("sky", "natural", "Yulduzli osmon"),
    PatternDef("paper", "natural", "Qogʻoz"),
)

@Immutable
data class Choice(val value: String, val label: String)

/** Naqsh va yog'du kuchi: naqsh rangi — matn rangining shuncha foizi */
val STRENGTHS = listOf(Choice("low", "Past"), Choice("medium", "Oʻrta"), Choice("high", "Kuchli"))
private val STRENGTH_PERCENT = mapOf("low" to 4, "medium" to 7, "high" to 11)

/** Naqsh o'lchami — katakning kattaligi (chiziqlar qalinligi o'zgarmaydi) */
val SIZES = listOf(Choice("small", "Kichik"), Choice("medium", "Oʻrta"), Choice("large", "Katta"))
private val SIZE_SCALE = mapOf("small" to 0.7f, "medium" to 1f, "large" to 1.5f)

/** Burchaklar: veb nusxadagi yagona --radius-md (hamma narsa rounded-md) */
val RADII = listOf(
    Choice("sharp", "Oʻtkir"),
    Choice("default", "Standart"),
    Choice("soft", "Yumaloq"),
    Choice("round", "Juda yumaloq"),
)
private val RADIUS_DP = mapOf("sharp" to 2.dp, "default" to 6.dp, "soft" to 10.dp, "round" to 14.dp)

@Immutable
data class CardStyleDef(val id: String, val name: String, val hint: String)

val CARD_STYLES = listOf(
    CardStyleDef("flat", "Tekis", "Faqat chegara"),
    CardStyleDef("shadow", "Soyali", "Yengil koʻtarilgan"),
    CardStyleDef("glass", "Shaffof", "Orqa fon koʻrinib turadi"),
    CardStyleDef("borderless", "Chegarasiz", "Faqat rang bilan ajraladi"),
)

/** Saqlanadigan tanlov — DataStore'da har biri alohida kalit */
@Immutable
data class Appearance(
    val theme: String = "default",
    val accent: String = "theme",
    val pattern: String = "none",
    val strength: String = "medium",
    val size: String = "medium",
    val tint: Boolean = false,
    val glow: Boolean = false,
    val cards: String = "flat",
    val radius: String = "default",
) {
    val isDefault: Boolean get() = this == Appearance()

    val themeDef: ThemeDef get() = THEMES.firstOrNull { it.id == theme } ?: THEMES.first()
    val accentDef: AccentDef get() = ACCENTS.firstOrNull { it.id == accent } ?: ACCENTS.first()
    val patternDef: PatternDef get() = PATTERNS.firstOrNull { it.id == pattern } ?: PATTERNS.first()
    val cardStyle: String get() = CARD_STYLES.firstOrNull { it.id == cards }?.id ?: "flat"

    val radiusDp: Dp get() = RADIUS_DP[radius] ?: 6.dp
    val strengthPercent: Int get() = STRENGTH_PERCENT[strength] ?: 7
    val patternScale: Float get() = SIZE_SCALE[size] ?: 1f
}

/** Tanlangan mavzu + asosiy rang → ilovaning to'liq ranglari */
fun holyColors(value: Appearance, dark: Boolean): HolyColors {
    var p = value.themeDef.let { if (dark) it.dark else it.light }
    val accent = value.accentDef
    if (accent.id != "theme") {
        val color = (if (dark) accent.dark else accent.light) ?: p.accent
        p = p.copy(
            accent = color,
            accentHover = if (dark) mixOklab(color, Color.White, 0.75f) else mixOklab(color, Color.Black, 0.82f),
            accentSoft = mixOklab(color, p.surface, if (dark) 0.20f else 0.12f),
        )
    }
    return HolyColors(
        bg = p.bg,
        surface = p.surface,
        surface2 = p.surface2,
        line = p.line,
        ink = p.ink,
        inkSoft = p.inkSoft,
        inkFaint = p.inkFaint,
        accent = p.accent,
        accentHover = p.accentHover,
        accentSoft = p.accentSoft,
        read = EMERALD_500,
        danger = if (dark) Color(0xFFF87171) else Color(0xFFEF4444),
        isDark = dark,
    )
}

/** Yorug' / qorong'i rejimdagi asosiy rang — ranglar doirachasi uchun */
fun accentColor(value: Appearance, accentId: String, dark: Boolean): Color {
    val accent = ACCENTS.firstOrNull { it.id == accentId } ?: ACCENTS.first()
    return if (accent.id == "theme") {
        value.themeDef.let { if (dark) it.dark.accent else it.light.accent }
    } else {
        (if (dark) accent.dark else accent.light) ?: Color.Gray
    }
}

/** Tailwind emerald-500 — veb nusxada o'qilgan belgilar shu rangda */
val EMERALD_500 = Color(0xFF10B981)

/* ---------- color-mix(in oklab, a p%, b) ---------- */

private fun srgbToLinear(c: Float): Float =
    if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

private fun linearToSrgb(c: Float): Float =
    if (c <= 0.0031308f) c * 12.92f else (1.055f * c.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f)

private fun toOklab(color: Color): FloatArray {
    val r = srgbToLinear(color.red)
    val g = srgbToLinear(color.green)
    val b = srgbToLinear(color.blue)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

private fun fromOklab(lab: FloatArray, alpha: Float): Color {
    val l = lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2]
    val m = lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2]
    val s = lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2]
    val l3 = l * l * l
    val m3 = m * m * m
    val s3 = s * s * s
    val r = 4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3
    val g = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3
    val b = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3
    return Color(
        linearToSrgb(r).coerceIn(0f, 1f),
        linearToSrgb(g).coerceIn(0f, 1f),
        linearToSrgb(b).coerceIn(0f, 1f),
        alpha,
    )
}

/** CSS color-mix(in oklab, a [weight], b) — weight a ning ulushi */
fun mixOklab(a: Color, b: Color, weight: Float): Color {
    val la = toOklab(a)
    val lb = toOklab(b)
    val t = weight.coerceIn(0f, 1f)
    return fromOklab(
        floatArrayOf(
            la[0] * t + lb[0] * (1 - t),
            la[1] * t + lb[1] * (1 - t),
            la[2] * t + lb[2] * (1 - t),
        ),
        a.alpha * t + b.alpha * (1 - t),
    )
}
