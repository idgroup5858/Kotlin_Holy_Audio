package com.example.kotlin_holy.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

/** O'qish matni o'lchamlari — Sozlamalar → Matn o'lchami (1 = standart, 0.8…1.8) */
@Immutable
data class TextScales(val arabic: Float = 1f, val uz: Float = 1f)

val LocalTextScales = staticCompositionLocalOf { TextScales() }

/**
 * Veb nusxadagi index.css o'qish klasslari:
 *   .txt-uz          17px · 1.9 · justify      — o'zbekcha tarjima
 *   .txt-uz-sub      14px · 1.75 · justify     — transkripsiya, tafsir, izoh
 *   .txt-uz-heading  18px · 1.5                — kitob ichidagi sarlavhalar
 *   .txt-arabic      27px · 2.3 · o'ngdan      — oyat / hadis arabchasi
 * Hammasi sozlamadagi o'lchamga ko'paytiriladi.
 */
object ReadingText {
    val uz: TextStyle
        @Composable get() {
            val s = LocalTextScales.current.uz
            return TextStyle(
                fontFamily = JakartaSans,
                fontSize = (17 * s).sp,
                lineHeight = (17 * 1.9f * s).sp,
                textAlign = TextAlign.Justify,
                color = LocalHolyColors.current.inkSoft,
            )
        }

    val uzSub: TextStyle
        @Composable get() {
            val s = LocalTextScales.current.uz
            return TextStyle(
                fontFamily = JakartaSans,
                fontSize = (14 * s).sp,
                lineHeight = (14 * 1.75f * s).sp,
                textAlign = TextAlign.Justify,
                color = LocalHolyColors.current.inkFaint,
            )
        }

    val uzHeading: TextStyle
        @Composable get() {
            val s = LocalTextScales.current.uz
            return TextStyle(
                fontFamily = JakartaSans,
                fontSize = (18 * s).sp,
                lineHeight = (18 * 1.5f * s).sp,
                fontWeight = FontWeight.Bold,
                color = LocalHolyColors.current.ink,
            )
        }

    /** Arabcha matn: [family] — Amiri Quran (.arabic) yoki KFGQPC Hafs (.arabic-hadith) */
    @Composable
    fun arabic(family: FontFamily = AmiriQuran, baseSize: Float = 27f, lineHeight: Float = 2.3f): TextStyle {
        val s = LocalTextScales.current.arabic
        return TextStyle(
            fontFamily = family,
            fontSize = (baseSize * s).sp,
            lineHeight = (baseSize * lineHeight * s).sp,
            textAlign = TextAlign.Right,
            textDirection = TextDirection.Rtl,
            color = LocalHolyColors.current.ink,
        )
    }
}
