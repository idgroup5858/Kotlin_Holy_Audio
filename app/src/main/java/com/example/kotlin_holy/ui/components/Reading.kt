package com.example.kotlin_holy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.theme.HolyTheme
import java.util.Calendar

/** 19.09.2026 22:05 */
fun formatReadAt(timestamp: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = timestamp }
    fun two(value: Int) = value.toString().padStart(2, '0')
    return "${two(c.get(Calendar.DAY_OF_MONTH))}.${two(c.get(Calendar.MONTH) + 1)}.${c.get(Calendar.YEAR)} " +
        "${two(c.get(Calendar.HOUR_OF_DAY))}:${two(c.get(Calendar.MINUTE))}"
}

/** O'qilgan kartochka / qator — o'qilgan sura kabi yashil (READ_TILE / READ_ROW) */
@Composable
fun readTileBorder(): Color = HolyTheme.colors.read.copy(alpha = 0.7f)

@Composable
fun readTileBackground(): Color = HolyTheme.colors.read.copy(alpha = 0.07f)

/** «O'qildi» tugmasi — mushaf betida ham, hadisda ham bir xil (ReadMarkButton) */
@Composable
fun ReadMarkButton(readAt: Long?, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val done = readAt != null
    Column(
        modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier
                .clip(shape)
                .background(if (done) colors.read.copy(alpha = 0.10f) else colors.surface2)
                .border(1.dp, if (done) colors.read.copy(alpha = 0.7f) else colors.line, shape)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(if (done) colors.read else Color.Transparent)
                    .border(1.dp, if (done) colors.read else colors.inkFaint, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(HolyIcons.Check, null, tint = if (done) Color.White else Color.Transparent, modifier = Modifier.size(12.dp))
            }
            Text(
                if (done) "O'qildi" else "O'qildi deb belgilash",
                color = if (done) colors.read else colors.inkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (readAt != null) {
            Text(
                "${formatReadAt(readAt)} · belgini olib tashlash uchun qayta bosing",
                color = colors.read,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** «Oldingi / keyingi» tugmasining amali: matni va bosilganda nima bo'lishi */
data class PagerAction(val label: String, val onClick: () -> Unit)

/**
 * O'qish rejimiga mos «‹ oldingi · 3 / 604 · keyingi ›» navigatsiyasi (ReaderPager).
 * Tugma bo'lmasa o'rni bo'sh qoladi — o'rtadagi yozuv siljimaydi.
 */
@Composable
fun ReaderPager(prev: PagerAction?, next: PagerAction?, label: String?, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    FlowRow(
        modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        PagerButton(prev, leading = true)
        if (label != null) {
            Text(
                label,
                color = colors.inkFaint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 100.dp),
            )
        }
        PagerButton(next, leading = false)
    }
}

@Composable
private fun PagerButton(action: PagerAction?, leading: Boolean) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    if (action == null) {
        Box(Modifier.widthIn(min = 0.dp))
        return
    }
    Row(
        Modifier
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = action.onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading) Icon(HolyIcons.ChevronLeft, null, tint = colors.inkSoft, modifier = Modifier.size(16.dp))
        Text(action.label, color = colors.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (!leading) Icon(HolyIcons.ChevronRight, null, tint = colors.inkSoft, modifier = Modifier.size(16.dp))
    }
}

/** O'qish holati: { read, total, done } — sura, juz, bo'lim, kitob uchun umumiy */
data class ReadStatus(val read: Int, val total: Int) {
    val done: Boolean get() = total > 0 && read >= total
}

/** «12/148 hadis oʻqildi» yoki «✓ Oʻqildi». itemName — kitoblarda «hikmat» kabi */
fun readStatusLabel(status: ReadStatus, itemName: String = "hadis"): String =
    if (status.done) "✓ Oʻqildi" else "${status.read}/${status.total} $itemName oʻqildi"

/** Kitob yoki bo'lim sarlavhasidagi holat: «✓ To'liq o'qildi · 42 ta hadis» */
@Composable
fun ReadStatusBadge(status: ReadStatus, itemName: String = "hadis") {
    if (status.read <= 0) return
    HolyBadge(
        if (status.done) {
            "✓ Toʻliq oʻqildi · ${status.total} ta $itemName"
        } else {
            "${status.read}/${status.total} $itemName oʻqildi · ${status.total - status.read} ta qoldi"
        },
        tone = BadgeTone.Read,
    )
}

/** «O'qildi» doirachasi kartochka burchagida yoki qator ustunida (ToggleButton) */
@Composable
fun ReadToggle(
    done: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    partial: Boolean = false,
    color: Color? = null,
    size: androidx.compose.ui.unit.Dp = 20.dp,
    contentDescription: String? = null,
) {
    CheckCircle(
        checked = done,
        onClick = onToggle,
        modifier = modifier,
        size = size,
        color = color,
        partial = partial,
        contentDescription = contentDescription,
    )
}

/** Matnli havola (asosiy rangda) */
@Composable
fun LinkText(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        color = HolyTheme.colors.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.clip(HolyTheme.shape).clickable(onClick = onClick),
    )
}
