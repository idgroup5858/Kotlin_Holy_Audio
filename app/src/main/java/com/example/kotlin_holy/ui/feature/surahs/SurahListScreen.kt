package com.example.kotlin_holy.ui.feature.surahs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.OrderToggle
import com.example.kotlin_holy.ui.components.ReadCheckCircle
import com.example.kotlin_holy.ui.components.ScreenHeader
import com.example.kotlin_holy.ui.theme.HolyTheme

@Composable
fun SurahListScreen(
    onOpenSurah: (Int) -> Unit,
    viewModel: SurahListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Suralar",
                subtitle = "Qurʼonning 114 surasi — oʻqish uchun tanlang",
            )
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text("Sura qidirish…", color = colors.inkFaint) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = colors.inkFaint)
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.line,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.ink,
                    unfocusedTextColor = colors.ink,
                    cursorColor = colors.accent,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${state.visible.size} ta sura",
                    color = colors.inkFaint,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                OrderToggle(order = state.order, onToggle = viewModel::toggleOrder)
            }
        }

        if (state.loading) {
            item { LoadingRows(rows = 6) }
        } else if (state.visible.isEmpty()) {
            item {
                EmptyState(
                    title = "Hech narsa topilmadi",
                    hint = "Boshqa soʻz bilan qidirib koʻring.",
                )
            }
        } else {
            items(state.visible, key = { it.number }) { surah ->
                SurahRow(
                    surah = surah,
                    xatm = state.xatm[surah.number],
                    onClick = { onOpenSurah(surah.number) },
                    onToggle = { viewModel.toggleSurah(surah.number) },
                )
            }
        }
    }
}

@Composable
private fun SurahRow(
    surah: Surah,
    xatm: SurahXatm?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val done = xatm?.done == true

    HolyCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (done) colors.read.copy(alpha = 0.7f) else null,
        background = if (done) colors.read.copy(alpha = 0.07f) else null,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                /* Raqam romb ichida — veb nusxadagi Diamond */
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .rotate(45f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = surah.number.toString(),
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.rotate(-45f),
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        surah.nameUz,
                        color = colors.ink,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        surah.meaningUz ?: if (surah.isMakki) "Makkiy" else "Madaniy",
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (xatm != null && xatm.started) {
                        Text(
                            text = if (xatm.done) "✓ Oʻqildi" else "${xatm.read}/${xatm.total} bet oʻqildi",
                            color = colors.read,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                /* O'ng burchakdagi belgi doirasi bilan to'qnashmasligi uchun chekinish */
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = 20.dp),
                ) {
                    Text(
                        surah.nameArabic,
                        color = colors.ink,
                        fontSize = 18.sp,
                        maxLines = 1,
                    )
                    Text(
                        "${surah.totalAyahs} oyat",
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            ReadCheckCircle(
                checked = done,
                partial = xatm?.started == true && !done,
                onToggle = onToggle,
                size = 22.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}
