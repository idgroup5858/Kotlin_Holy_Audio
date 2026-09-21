package com.example.kotlin_holy.ui.theme

import android.content.res.AssetManager
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Arabcha matn uchun ikki shrift bor:
 *   uthmanic_hafs — oyat bo'yicha ko'rinishdagi oddiy arabcha matn;
 *   sura_names    — sura sarlavhasidagi xattotlik ("018 surah" → «سورة الكهف»).
 * Mushaf betlarining o'z shriftlari alohida yuklanadi (MushafFontProvider).
 */
object HolyFonts {
    fun uthmani(assets: AssetManager): FontFamily =
        FontFamily(Font("fonts/uthmanic_hafs.ttf", assets))

    fun surahNames(assets: AssetManager): FontFamily =
        FontFamily(Font("fonts/sura_names.ttf", assets))
}

val HolyTypography = Typography(
    titleLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 23.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
)
