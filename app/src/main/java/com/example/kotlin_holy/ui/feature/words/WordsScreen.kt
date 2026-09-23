package com.example.kotlin_holy.ui.feature.words

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.DifficultWord
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyTextField
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.ScreenHeader
import com.example.kotlin_holy.ui.theme.HolyFonts
import com.example.kotlin_holy.ui.theme.HolyTheme

@Composable
fun WordsScreen(
    onOpenSurah: (Int) -> Unit,
    viewModel: WordsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val context = LocalContext.current
    val arabicFamily = remember { HolyFonts.uthmani(context.assets) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Qiyin soʻzlar",
                subtitle = "Qurʼondagi oʻqilishi ogʻir soʻzlar va ularning talaffuzi",
            )
        }

        item {
            HolyTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = "Soʻz qidirish…",
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = colors.inkFaint,
                        modifier = Modifier.size(18.dp),
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
        }

        when {
            state.loading -> item { LoadingRows(rows = 6) }
            state.visible.isEmpty() -> item {
                EmptyState(title = "Soʻz topilmadi", hint = "Boshqa soʻz bilan qidirib koʻring.")
            }

            else -> items(state.visible, key = { it.id }) { word ->
                WordCard(word = word, arabicFamily = arabicFamily, onOpenSurah = onOpenSurah)
            }
        }
    }
}

@Composable
private fun WordCard(
    word: DifficultWord,
    arabicFamily: androidx.compose.ui.text.font.FontFamily,
    onOpenSurah: (Int) -> Unit,
) {
    val colors = HolyTheme.colors

    HolyCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = word.firstSurah?.let { { onOpenSurah(it) } },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (word.transliteration != null) {
                    Text(
                        word.transliteration,
                        color = colors.ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HolyBadge("${word.count} marta")
                    if (word.firstSurah != null && word.firstAyah != null) {
                        HolyBadge(
                            "${word.firstSurah}:${word.firstAyah}",
                            color = colors.accent,
                            background = colors.accent.copy(alpha = 0.12f),
                        )
                    }
                }
            }
            Text(
                word.text,
                fontFamily = arabicFamily,
                color = colors.ink,
                fontSize = 24.sp,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
