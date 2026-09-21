package com.example.kotlin_holy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.theme.HolyTheme
import java.util.Calendar

/** Kartochka burchagidagi kichik «o'qildi» belgisi */
@Composable
fun ReadCheckCircle(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    partial: Boolean = false,
) {
    val colors = HolyTheme.colors
    val border = when {
        checked -> colors.read
        partial -> colors.read.copy(alpha = 0.6f)
        else -> colors.line
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) colors.read else Color.Transparent)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (checked) "Belgini olib tashlash" else "Oʻqildi deb belgilash",
            tint = when {
                checked -> Color.White
                partial -> colors.read.copy(alpha = 0.6f)
                else -> Color.Transparent
            },
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

/** Betni o'qib bo'lgach, betning pastida turadigan tugma */
@Composable
fun PageReadMark(
    readAt: Long?,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val done = readAt != null

    Column(
        modifier = modifier.fillMaxWidth().padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (done) colors.read.copy(alpha = 0.12f) else colors.surface2)
                .border(
                    1.dp,
                    if (done) colors.read.copy(alpha = 0.7f) else colors.line,
                    CircleShape,
                )
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReadCheckCircle(checked = done, onToggle = onToggle, size = 20.dp)
            Text(
                text = if (done) "Oʻqildi" else "Oʻqildi deb belgilash",
                color = if (done) colors.read else colors.inkSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (readAt != null) {
            Text(
                text = "${formatReadAt(readAt)} · belgini olib tashlash uchun qayta bosing",
                color = colors.read,
                fontSize = 11.sp,
            )
        }
    }
}

/** 19.09.2026 22:05 ko'rinishidagi sana */
fun formatReadAt(timestamp: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun two(value: Int) = value.toString().padStart(2, '0')
    return "${two(calendar.get(Calendar.DAY_OF_MONTH))}.${two(calendar.get(Calendar.MONTH) + 1)}." +
        "${calendar.get(Calendar.YEAR)} ${two(calendar.get(Calendar.HOUR_OF_DAY))}:" +
        two(calendar.get(Calendar.MINUTE))
}
