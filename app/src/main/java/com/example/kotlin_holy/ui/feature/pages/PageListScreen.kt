package com.example.kotlin_holy.ui.feature.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
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
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.FilterChip
import com.example.kotlin_holy.ui.components.HolyTextField
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.OrderToggle
import com.example.kotlin_holy.ui.components.ReadCheckCircle
import com.example.kotlin_holy.ui.components.ScreenHeader
import com.example.kotlin_holy.ui.components.formatReadAt
import com.example.kotlin_holy.ui.theme.HolyTheme

@Composable
fun PageListScreen(
    onOpenPage: (Int) -> Unit,
    viewModel: PageListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 108.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            Column {
                ScreenHeader(
                    title = "Sahifalar",
                    subtitle = "Mushaf boʻyicha 1–604 sahifa",
                )
                PageJumpField(onJump = onOpenPage)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        label = "Faqat oʻqilmaganlar",
                        selected = state.onlyUnread,
                        onClick = viewModel::toggleOnlyUnread,
                    )
                    Box(Modifier.weight(1f))
                    OrderToggle(order = state.order, onToggle = viewModel::toggleOrder)
                }
            }
        }

        if (state.loading) {
            item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                LoadingRows(rows = 5)
            }
        } else {
            items(state.visible, key = { it.number }) { page ->
                val info = state.surahByPage[page.number]
                PageCard(
                    number = page.number,
                    ayahCount = page.ayahCount,
                    startingLabel = info?.starting?.firstOrNull()?.let { first ->
                        val extra = (info.starting.size - 1).takeIf { it > 0 }
                        "${first.number}. ${first.nameUz}" + (extra?.let { " +$it" } ?: "")
                    },
                    continuingLabel = info?.current?.nameUz,
                    startsSurah = info?.starting?.isNotEmpty() == true,
                    readAt = state.readPages[page.number],
                    onClick = { onOpenPage(page.number) },
                    onToggle = { viewModel.toggle(page.number) },
                )
            }
        }
    }
}

@Composable
private fun PageCard(
    number: Int,
    ayahCount: Int,
    startingLabel: String?,
    continuingLabel: String?,
    startsSurah: Boolean,
    readAt: Long?,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val read = readAt != null

    HolyCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = when {
            read -> colors.read.copy(alpha = 0.7f)
            startsSurah -> colors.accent.copy(alpha = 0.45f)
            else -> null
        },
        background = when {
            read -> colors.read.copy(alpha = 0.07f)
            startsSurah -> colors.accent.copy(alpha = 0.05f)
            else -> null
        },
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    number.toString(),
                    color = colors.ink,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("$ayahCount oyat", color = colors.inkFaint, fontSize = 11.sp)

                if (startingLabel != null) {
                    Text(
                        text = startingLabel,
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.accent.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                } else if (continuingLabel != null) {
                    Text(
                        text = continuingLabel,
                        color = colors.inkSoft,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (readAt != null) {
                    Text(
                        text = formatReadAt(readAt),
                        color = colors.read,
                        fontSize = 10.sp,
                        maxLines = 1,
                    )
                }
            }

            ReadCheckCircle(
                checked = read,
                onToggle = onToggle,
                size = 22.dp,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            )

            if (startsSurah) {
                Text(
                    "۞",
                    color = colors.accent,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** «Sahifa №» maydoni: raqam kiritilsa, oʻsha bet ochiladi */
@Composable
private fun PageJumpField(onJump: (Int) -> Unit) {
    val colors = HolyTheme.colors
    var value by remember { mutableStateOf("") }

    fun jump() {
        val number = value.toIntOrNull() ?: return
        if (number in 1..604) onJump(number)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HolyTextField(
            value = value,
            onValueChange = { text -> value = text.filter { it.isDigit() }.take(3) },
            placeholder = "Sahifa №",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(120.dp),
        )
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.accent)
                .clickable { jump() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                "Oʻtish",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
