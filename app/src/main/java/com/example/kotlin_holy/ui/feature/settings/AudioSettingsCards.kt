package com.example.kotlin_holy.ui.feature.settings

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.components.formatBytes
import com.example.kotlin_holy.ui.theme.HolyTheme

/** Qorilar ro'yxatidagi bitta qator */
@Composable
internal fun ReciterRow(
    nameUz: String,
    style: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = HolyTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(if (selected) colors.accent.copy(alpha = 0.10f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
                .border(1.dp, if (selected) colors.accent else colors.line, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.bg,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Text(
            text = nameUz,
            color = if (selected) colors.ink else colors.inkSoft,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        Text(style, color = colors.inkFaint, fontSize = 12.sp)
    }
}

/**
 * Qurilmada saqlangan qiroatlar.
 *
 * Audio sura-ba-sura yuklangani uchun bu yerda ham har bir sura alohida
 * o'chiriladi — shunda kerak bo'lmagani olib tashlanib, papka hajmi kamayadi.
 */
@Composable
internal fun RecitationStorageCard(
    storage: AudioStorage,
    reciterLabel: String,
    onDelete: (Int) -> Unit,
    onDeleteAll: () -> Unit,
) {
    val colors = HolyTheme.colors
    var confirming by remember { mutableStateOf(false) }

    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            SectionTitle("Yuklangan qiroatlar")

            Text(
                text = if (storage.totalBytes > 0) {
                    "${formatBytes(storage.totalBytes)} joy egallangan"
                } else {
                    "Hali birorta sura yuklanmagan."
                },
                color = colors.inkSoft,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = "Qiroat sura ekranidan yuklanadi — $reciterLabel",
                color = colors.inkFaint,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )

            storage.recitations.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "${item.surahNumber}. ${item.nameUz}",
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatBytes(item.bytes), color = colors.inkFaint, fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(colors.surface2)
                            .clickable { onDelete(item.surahNumber) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "${item.nameUz} qiroatini oʻchirish",
                            tint = colors.danger,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            if (storage.totalBytes > 0) {
                if (confirming) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "Hammasi oʻchirilsinmi?",
                            color = colors.ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Ha, oʻchirish",
                            color = colors.danger,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable {
                                    onDeleteAll()
                                    confirming = false
                                }
                                .padding(6.dp),
                        )
                        Text(
                            "Bekor qilish",
                            color = colors.inkSoft,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { confirming = false }.padding(6.dp),
                        )
                    }
                } else {
                    Text(
                        "Barcha qiroatlarni oʻchirish",
                        color = colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(CircleShape)
                            .background(colors.surface2)
                            .clickable { confirming = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
