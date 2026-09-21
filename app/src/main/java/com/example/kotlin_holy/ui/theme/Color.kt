package com.example.kotlin_holy.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Ranglar veb nusxadagi (ReactJS_Holy) qiymatlarning aynan o'zi.
 * Material 3 sxemasi ustiga o'z ranglarimizni qo'shamiz, chunki ilovada
 * "surface-2", "line", "ink-faint" kabi qo'shimcha tuslar ham ishlatiladi.
 */
@Immutable
data class HolyColors(
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
    val read: Color,
    val danger: Color,
    val isDark: Boolean,
)

val LightHolyColors = HolyColors(
    bg = Color(0xFFF6F6F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF0F0F2),
    line = Color(0xFFE3E3E7),
    ink = Color(0xFF17181B),
    inkSoft = Color(0xFF55565E),
    inkFaint = Color(0xFF9B9CA5),
    accent = Color(0xFF157C85),
    accentHover = Color(0xFF10646C),
    accentSoft = Color(0xFFE4F2F3),
    read = Color(0xFF10B981),
    danger = Color(0xFFEF4444),
    isDark = false,
)

val DarkHolyColors = HolyColors(
    bg = Color(0xFF0F1012),
    surface = Color(0xFF1C1D21),
    surface2 = Color(0xFF26272C),
    line = Color(0xFF2C2D34),
    ink = Color(0xFFF2F2F4),
    inkSoft = Color(0xFFA0A1AA),
    inkFaint = Color(0xFF6E6F79),
    accent = Color(0xFF4EC3CC),
    accentHover = Color(0xFF6AD3DB),
    accentSoft = Color(0xFF14343A),
    read = Color(0xFF10B981),
    danger = Color(0xFFF87171),
    isDark = true,
)
