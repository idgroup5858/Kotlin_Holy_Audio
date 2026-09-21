package com.example.kotlin_holy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.feature.memorization.MemorizationUiState
import com.example.kotlin_holy.ui.theme.HolyTheme

/**
 * Oyatni yodlash paneli: mikrofon tugmasi, ketma-ket toʻgʻri oʻqishlar
 * hisoblagichi va oxirgi urinish natijasi.
 */
@Composable
fun MemorizationPanel(
    state: MemorizationUiState,
    onMicClick: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface2)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (state.memorized) {
                    "Yodlandi ✓"
                } else {
                    "Ketma-ket toʻgʻri: ${state.streak}/${state.target}"
                },
                color = if (state.memorized) colors.read else colors.inkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Box(Modifier.weight(1f))
            if (state.streak > 0 || state.memorized) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = "Qayta boshlash",
                    tint = colors.inkFaint,
                    modifier = Modifier.size(18.dp).clickable(onClick = onReset),
                )
                Box(Modifier.width(14.dp))
            }
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Yopish",
                tint = colors.inkFaint,
                modifier = Modifier.size(18.dp).clickable(onClick = onClose),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (state.listening) colors.accent else colors.accent.copy(alpha = 0.15f),
                    )
                    .clickable(onClick = onMicClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Oʻqishni boshlash",
                    tint = if (state.listening) colors.bg else colors.accent,
                    modifier = Modifier.size(22.dp),
                )
            }

            Text(
                text = when {
                    state.listening -> "Tinglanmoqda... oʻqing"
                    state.error != null -> state.error
                    state.lastResult == true -> "✓ Toʻgʻri oʻqildingiz"
                    state.lastResult == false -> "✗ Xato — qaytadan urinib koʻring"
                    else -> "Mikrofonni bosib oyatni oʻqing"
                },
                color = when {
                    state.error != null || state.lastResult == false -> colors.danger
                    state.lastResult == true -> colors.read
                    else -> colors.inkFaint
                },
                fontSize = 12.sp,
            )
        }
    }
}
