package com.example.kotlin_holy.ui.feature.juz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.JuzInfo
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.ScreenHeader
import com.example.kotlin_holy.ui.theme.HolyTheme

@Composable
fun JuzListScreen(
    onOpenJuz: (Int) -> Unit,
    viewModel: JuzListViewModel = hiltViewModel(),
) {
    val names by viewModel.surahNames.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ScreenHeader(
                title = "Juzlar",
                subtitle = "Har bir juz qaysi suralar, oyatlar va betlarni oʻz ichiga oladi",
            )
        }

        items(com.example.kotlin_holy.domain.model.JuzCatalog.all, key = { it.number }) { juz ->
            JuzCard(
                juz = juz,
                fromName = names[juz.fromSurah],
                toName = names[juz.toSurah],
                onClick = { onOpenJuz(juz.number) },
            )
        }
    }
}

@Composable
private fun JuzCard(juz: JuzInfo, fromName: String?, toName: String?, onClick: () -> Unit) {
    val colors = HolyTheme.colors

    HolyCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                juz.number.toString(),
                color = colors.ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            val names = listOfNotNull(fromName, toName.takeIf { juz.fromSurah != juz.toSurah })
            if (names.isNotEmpty()) {
                Text(
                    names.joinToString(" — "),
                    color = colors.inkSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                "${juz.fromSurah}:${juz.fromAyah} – ${juz.toSurah}:${juz.toAyah}",
                color = colors.inkFaint,
                fontSize = 11.sp,
            )
            Text(
                "${juz.fromPage}–${juz.toPage}-bet",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Text("${juz.ayahCount} oyat", color = colors.inkFaint, fontSize = 11.sp)
        }
    }
}
