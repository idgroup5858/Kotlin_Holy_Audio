package com.example.kotlin_holy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.ui.theme.HolyTheme

/** 115.3 MB ko'rinishidagi hajm */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> "%.0f MB".format(bytes / 1024.0 / 1024)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/**
 * Suraning qiroati: yuklash, jarayon va o'chirish.
 *
 * Audio butun Qur'on uchun emas, faqat shu sura uchun yuklanadi — shuning
 * uchun tugma sura ekranida turadi va yonida egallanadigan joy ko'rsatiladi.
 */
@Composable
fun AudioDownloadCard(
    state: AudioDownload,
    expectedBytes: Long?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    /** Betda bir nechta sura bo'lsa, qaysi suraniki ekanini ajratish uchun */
    title: String? = null,
) {
    val colors = HolyTheme.colors

    HolyCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            when (state) {
                is AudioDownload.Absent -> {
                    val size = state.expectedBytes ?: expectedBytes
                    AudioActionRow(
                        icon = Icons.Filled.Download,
                        title = title ?: "Qiroatni yuklash",
                        subtitle = if (size != null) {
                            "Taxminan ${formatBytes(size)} · keyin internetsiz ishlaydi"
                        } else {
                            "Yuklangach internetsiz tinglanadi"
                        },
                        actionLabel = "Yuklash",
                        onAction = onDownload,
                    )
                }

                is AudioDownload.InProgress -> {
                    val percent by animateFloatAsState(
                        targetValue = state.percent / 100f,
                        label = "audio-progress",
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Yuklanmoqda… ${state.percent}%",
                            color = colors.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        IconCircleButton(
                            icon = Icons.Filled.Close,
                            contentDescription = "Yuklashni toʻxtatish",
                            onClick = onCancel,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(colors.surface2),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(percent.coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(colors.accent),
                        )
                    }
                    val total = state.totalBytes
                    Text(
                        text = if (total != null) {
                            "${formatBytes(state.downloadedBytes)} / ${formatBytes(total)}"
                        } else {
                            formatBytes(state.downloadedBytes)
                        },
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                is AudioDownload.Ready -> {
                    AudioActionRow(
                        icon = Icons.Filled.Delete,
                        title = title ?: "Qiroat yuklangan",
                        subtitle = "${formatBytes(state.bytes)} joy egallagan",
                        actionLabel = "Oʻchirish",
                        onAction = onDelete,
                        danger = true,
                    )
                }

                is AudioDownload.Failed -> {
                    AudioActionRow(
                        icon = Icons.Filled.Download,
                        title = "Yuklab boʻlmadi",
                        subtitle = state.reason ?: "Internet aloqasini tekshiring",
                        actionLabel = "Qayta urinish",
                        onAction = onDownload,
                        danger = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    danger: Boolean = false,
) {
    val colors = HolyTheme.colors
    val tint = if (danger) colors.danger else colors.accent

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = colors.inkFaint, fontSize = 12.sp)
        }
        Text(
            text = actionLabel,
            color = if (danger) colors.danger else colors.bg,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(if (danger) Color.Transparent else colors.accent)
                .border(
                    1.dp,
                    if (danger) colors.danger.copy(alpha = 0.5f) else Color.Transparent,
                    CircleShape,
                )
                .clickable(onClick = onAction)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
