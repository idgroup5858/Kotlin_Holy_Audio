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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.ui.theme.HolyTheme

/** 115.3 MB ko'rinishidagi hajm */
fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024 / 1024)
    bytes >= 1024L * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

/**
 * Suraning qiroati: yuklash, jarayon va o'chirish (faqat Android nusxasida — veb nusxada
 * audio yo'q). Ko'rinishi boshqa kartochkalar bilan bir xil uslubda.
 * Audio butun Qur'on uchun emas, faqat shu sura uchun yuklanadi.
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            when (state) {
                is AudioDownload.Absent -> {
                    val size = state.expectedBytes ?: expectedBytes
                    AudioRow(
                        icon = HolyIcons.Headphones,
                        title = title ?: "Qiroatni yuklab olish",
                        subtitle = if (size != null) {
                            "Taxminan ${formatBytes(size)} · keyin internetsiz tinglanadi"
                        } else {
                            "Yuklangach internetsiz tinglanadi"
                        },
                    ) {
                        HolyTextButton("Yuklash", onDownload, variant = ButtonVariant.Primary, size = ButtonSize.Sm, icon = HolyIcons.Download)
                    }
                }

                is AudioDownload.InProgress -> {
                    AudioRow(
                        icon = HolyIcons.Download,
                        title = "Yuklanmoqda… ${state.percent}%",
                        subtitle = state.totalBytes?.let { "${formatBytes(state.downloadedBytes)} / ${formatBytes(it)}" }
                            ?: formatBytes(state.downloadedBytes),
                    ) {
                        HolyTextButton("Toʻxtatish", onCancel, size = ButtonSize.Sm)
                    }
                    HolyProgress(
                        value = state.percent,
                        total = 100,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                is AudioDownload.Ready -> {
                    AudioRow(
                        icon = HolyIcons.Headphones,
                        title = title?.let { "$it · yuklangan" } ?: "Qiroat yuklangan",
                        subtitle = "${formatBytes(state.bytes)} · oyat raqamini bosib tinglang",
                        iconTint = colors.read,
                    ) {
                        HolyTextButton("Oʻchirish", onDelete, variant = ButtonVariant.Danger, size = ButtonSize.Sm)
                    }
                }

                is AudioDownload.Failed -> {
                    AudioRow(
                        icon = HolyIcons.Download,
                        title = "Yuklab boʻlmadi",
                        subtitle = state.reason ?: "Internet aloqasini tekshiring",
                        iconTint = Color(0xFFEF4444),
                    ) {
                        HolyTextButton("Qayta urinish", onDownload, variant = ButtonVariant.Primary, size = ButtonSize.Sm)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color? = null,
    action: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    val tint = iconTint ?: colors.accent
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = colors.inkFaint, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        action()
    }
}

/** Oyat qatoridagi kichik qiroat tugmalari: o'qish / to'xtatib turish va to'xtatish */
@Composable
fun AyahAudioButtons(isPlaying: Boolean, onPlay: () -> Unit, onPause: () -> Unit, onStop: () -> Unit) {
    val colors = HolyTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(28.dp)
                .clip(HolyTheme.shape)
                .background(if (isPlaying) colors.accent else colors.accent.copy(alpha = 0.12f))
                .clickable { if (isPlaying) onPause() else onPlay() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) HolyIcons.Pause else HolyIcons.Play,
                if (isPlaying) "Toʻxtatib turish" else "Oyatni oʻqish",
                tint = if (isPlaying) Color.White else colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
        if (isPlaying) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(HolyTheme.shape)
                    .background(colors.surface2)
                    .clickable(onClick = onStop),
                contentAlignment = Alignment.Center,
            ) {
                Icon(HolyIcons.Stop, "Toʻxtatish", tint = colors.inkSoft, modifier = Modifier.size(13.dp))
            }
        }
    }
}
