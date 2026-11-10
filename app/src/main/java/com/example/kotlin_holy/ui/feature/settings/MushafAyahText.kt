package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.ui.components.rememberMushafFont
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.LocalTextScales
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Namunadagi oyatlar uchun mushaf betlari — bir marta o'qiladi */
@HiltViewModel
class MushafSampleViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
) : ViewModel() {

    /* O'qib bo'lmagan bet bo'sh bet sifatida saqlanadi — u holda zaxira matn chiqadi */
    private val _pages = MutableStateFlow<Map<Int, MushafPage>>(emptyMap())
    val pages: StateFlow<Map<Int, MushafPage>> = _pages.asStateFlow()
    private val requested = HashSet<Int>()

    fun load(page: Int) {
        if (!requested.add(page)) return
        viewModelScope.launch {
            val result = runCatching { quranRepository.mushafPage(page) }.getOrNull()
                ?: MushafPage(page, emptyList())
            _pages.update { it + (page to result) }
        }
    }
}

/* .txt-mushaf-ayah: calc(30px * var(--arabic-scale)) */
private const val MUSHAF_AYAH_FONT = 30f

/**
 * Veb nusxadagi MushafAyah: oyat o'zi turgan mushaf betining KFGQPC shriftida,
 * oyat raqami asosiy rangda. Bet topilmasa [fallback] ko'rsatiladi, yuklanguncha
 * esa joyi ko'rinmas zaxira matn bilan band turadi — sahifa sakramasligi uchun.
 */
@Composable
internal fun MushafAyahText(
    surahNumber: Int,
    ayahNumber: Int,
    pageNumber: Int,
    lineHeight: Float,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit = {},
) {
    val viewModel: MushafSampleViewModel = hiltViewModel()
    LaunchedEffect(pageNumber) { viewModel.load(pageNumber) }
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val page = pages[pageNumber]

    if (page == null) {
        Box(modifier.alpha(0f)) { fallback() }
        return
    }
    val words = remember(page, surahNumber, ayahNumber) {
        page.lines.flatMap { it.words }.filter { it.surahNumber == surahNumber && it.ayahNumber == ayahNumber }
    }
    if (words.isEmpty()) {
        Box(modifier) { fallback() }
        return
    }

    val colors = HolyTheme.colors
    val (family, _) = rememberMushafFont(pageNumber)
    val size = MUSHAF_AYAH_FONT * LocalTextScales.current.arabic
    val text = remember(words, colors) {
        buildAnnotatedString {
            words.forEachIndexed { index, word ->
                if (index > 0) append(' ')
                withStyle(SpanStyle(color = if (word.isEnd) colors.accent else colors.ink)) { append(word.code) }
            }
        }
    }
    Text(
        text = text,
        style = TextStyle(
            fontFamily = family,
            fontSize = size.sp,
            lineHeight = (size * lineHeight).sp,
            textAlign = TextAlign.Right,
            textDirection = TextDirection.Rtl,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
