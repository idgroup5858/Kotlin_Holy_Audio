package com.example.kotlin_holy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.ui.theme.HolyTheme

/**
 * Bet tarjimasi: oyat raqamlari matn ichida qalin bo'lib turadi.
 * Betda ikki sura bo'lsa (masalan 293-bet), ular nomi bilan ajratiladi —
 * aks holda «111.» dan keyin «1.» kelib, raqamlar chalkashadi.
 */
@Composable
fun PageTranslation(
    ayahs: List<Ayah>,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
) {
    val colors = HolyTheme.colors
    val segments = remember(ayahs) { ayahs.groupConsecutiveBySurah() }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        segments.forEach { segment ->
            Column(Modifier.fillMaxWidth()) {
                if (segments.size > 1) {
                    SurahDivider(
                        number = segment.surahNumber,
                        name = segment.surahName,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Text(
                    text = segment.toAnnotatedText(colors.ink),
                    color = colors.inkSoft,
                    fontSize = (16 * scale).sp,
                    lineHeight = (31 * scale).sp,
                )
            }
        }
    }
}

@Composable
private fun SurahDivider(number: Int, name: String?, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.weight(1f).height(1.dp).background(colors.line))
        Text(
            text = "$number. ${name ?: "sura"}",
            color = colors.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Box(Modifier.weight(1f).height(1.dp).background(colors.line))
    }
}

private data class TranslationSegment(
    val surahNumber: Int,
    val surahName: String?,
    val ayahs: List<Ayah>,
)

private fun List<Ayah>.groupConsecutiveBySurah(): List<TranslationSegment> {
    val segments = mutableListOf<TranslationSegment>()
    forEach { ayah ->
        val last = segments.lastOrNull()
        if (last != null && last.surahNumber == ayah.surahNumber) {
            segments[segments.lastIndex] = last.copy(ayahs = last.ayahs + ayah)
        } else {
            segments += TranslationSegment(ayah.surahNumber, ayah.surahNameUz, listOf(ayah))
        }
    }
    return segments
}

private fun TranslationSegment.toAnnotatedText(numberColor: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        ayahs.forEach { ayah ->
            withStyle(SpanStyle(color = numberColor, fontWeight = FontWeight.Bold)) {
                append("${ayah.numberInSurah}. ")
            }
            append(ayah.translationUz ?: "tarjima kiritilmagan")
            append("  ")
        }
    }
