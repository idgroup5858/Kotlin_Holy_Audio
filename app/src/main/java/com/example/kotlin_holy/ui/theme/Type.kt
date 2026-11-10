package com.example.kotlin_holy.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.R

/**
 * Shriftlar veb nusxadagidek:
 *   Plus Jakarta Sans — butun interfeys (--font-sans);
 *   Amiri Quran       — oddiy arabcha matn (.arabic: lug'at, eslatma, qidiruvdagi nomlar);
 *   uthmanic_hafs     — hadislar arabchasi (KFGQPC Hafs);
 *   sura_names        — sura sarlavhasidagi xattotlik ("018 surah" → «سورة الكهف»).
 * Mushaf betlarining o'z shriftlari alohida yuklanadi (MushafFontProvider).
 */
@OptIn(ExperimentalTextApi::class)
val JakartaSans: FontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(
            resId = R.font.plus_jakarta_sans,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

val AmiriQuran: FontFamily = FontFamily(Font(R.font.amiri_quran))

object HolyFonts {
    fun uthmani(assets: AssetManager): FontFamily =
        FontFamily(Font("fonts/uthmanic_hafs.ttf", assets))

    fun surahNames(assets: AssetManager): FontFamily =
        FontFamily(Font("fonts/sura_names.ttf", assets))
}

private val base = TextStyle(fontFamily = JakartaSans)

val HolyTypography = Typography(
    displayLarge = base.copy(fontSize = 36.sp),
    displayMedium = base.copy(fontSize = 32.sp),
    displaySmall = base.copy(fontSize = 30.sp),
    headlineLarge = base.copy(fontSize = 30.sp),
    headlineMedium = base.copy(fontSize = 26.sp),
    headlineSmall = base.copy(fontSize = 24.sp),
    titleLarge = base.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = base.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = base.copy(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = base.copy(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = base.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = base.copy(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
