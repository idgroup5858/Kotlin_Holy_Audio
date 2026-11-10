package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.components.Segmented
import com.example.kotlin_holy.ui.theme.HolyTheme
import java.util.Locale

/*
 * Sozlamalar tablari uchun umumiy bo'laklar — veb nusxadagi pages/settings papkasidagi
 * fayllarda takrorlanadigan kartochka, bo'lim sarlavhasi, tanlov katagi va tasdiqlash qatori.
 */

/* ---------------- Ekran eni (veb nusxadagi sm / lg) ---------------- */

private val SM = 640.dp
private val LG = 1024.dp
private val XL = 1440.dp

/**
 * Ustunlar soni — veb nusxadagi media so'rovlari kabi oyna eniga qarab
 * (grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 …). Telefonda doim [base].
 */
@Composable
internal fun columnsFor(base: Int, sm: Int = base, lg: Int = sm, xl: Int = lg): Int {
    val width = LocalConfiguration.current.screenWidthDp.dp
    return when {
        width >= XL -> xl
        width >= LG -> lg
        width >= SM -> sm
        else -> base
    }
}

/** lg: dan boshlab (≥ 1024dp) */
@Composable
internal fun isLarge(): Boolean = LocalConfiguration.current.screenWidthDp.dp >= LG

/* ---------------- Kartochka va bo'lim ---------------- */

/** Card className="p-6" */
@Composable
internal fun SettingsCard(
    modifier: Modifier = Modifier,
    padding: Dp = 24.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    HolyCard(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(padding), content = content)
    }
}

/** Kartochka ichidagi izoh: text-[13px] text-ink-faint (leading-relaxed bilan yoki usiz) */
@Composable
internal fun HintText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp,
    relaxed: Boolean = false,
    color: Color = HolyTheme.colors.inkFaint,
) {
    val value = fontSize.value
    Text(
        text,
        color = color,
        fontSize = fontSize,
        lineHeight = (value * if (relaxed) 1.625f else 1.5f).sp,
        modifier = modifier,
    )
}

/**
 * Mavzu tabidagi Section: sarlavha, izoh va o'ng tomondagi qo'shimcha (aside).
 * flex-wrap justify-between — tor ekranda aside izoh ostiga tushadi.
 */
@Composable
internal fun SettingsSection(
    title: String,
    hint: String,
    aside: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    SettingsCard {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                SectionTitle(title)
                HintText(hint, Modifier.padding(top = 4.dp))
            }
            if (aside != null) aside()
        }
        content()
    }
}

/** Bo'lim sarlavhasi yonidagi asosiy rangli yozuv: «Feruza», «Nuqtalar» */
@Composable
internal fun AsideLabel(text: String) {
    Text(text, color = HolyTheme.colors.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
}

/** Tor ekranda sig'masa suriladigan Segmented (veb nusxada u whitespace-nowrap) */
@Composable
internal fun ScrollableSegmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.horizontalScroll(rememberScrollState())) {
        Segmented(options = options, value = value, onChange = onChange)
    }
}

/* ---------------- Tanlov katagi (Option) ---------------- */

/**
 * Tanlanadigan katak — tanlangani asosiy rangli chegara, atrofidagi halqa (ring-2)
 * va o'ng yuqori burchakdan chiqib turgan belgi bilan.
 */
@Composable
internal fun OptionTile(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    label: String? = null,
    content: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val radius = HolyTheme.radius
    val accent = colors.accent
    Box(
        modifier
            /* Halqa va belgi kesilmasligi uchun clip'dan oldin chiziladi */
            .drawWithContent {
                if (selected) {
                    val ring = 2.dp.toPx()
                    drawRoundRect(
                        color = accent.copy(alpha = 0.25f),
                        topLeft = Offset(-ring / 2, -ring / 2),
                        size = Size(size.width + ring, size.height + ring),
                        cornerRadius = CornerRadius(radius.toPx() + ring / 2),
                        style = Stroke(ring),
                    )
                }
                drawContent()
                if (selected) drawCheckMark(accent, Offset(size.width + 2.dp.toPx(), 2.dp.toPx()))
            }
            .clip(shape)
            .background(if (selected) accent.copy(alpha = 0.08f) else Color.Transparent)
            .border(1.dp, if (selected) accent else colors.line, shape)
            .clickable(onClick = onClick)
            .semantics {
                this.selected = selected
                role = Role.Button
                if (label != null) contentDescription = label
            }
            .padding(contentPadding),
    ) {
        content()
    }
}

/** CheckMark: h-5 w-5 bg-accent doira va oq belgi (-top-2 -right-2) */
private fun DrawScope.drawCheckMark(accent: Color, center: Offset) {
    val r = 10.dp.toPx()
    drawCircle(Color.Black.copy(alpha = 0.10f), r + 1.dp.toPx() / 2, center + Offset(0f, 1.dp.toPx() / 2))
    drawCircle(accent, r, center)
    /* viewBox 24 → 12dp: M5 12.5l4.5 4.5L19 7.5 */
    val unit = 12.dp.toPx() / 24f
    val origin = center - Offset(6.dp.toPx(), 6.dp.toPx())
    val path = Path().apply {
        moveTo(origin.x + 5f * unit, origin.y + 12.5f * unit)
        lineTo(origin.x + 9.5f * unit, origin.y + 17f * unit)
        lineTo(origin.x + 19f * unit, origin.y + 7.5f * unit)
    }
    drawPath(path, Color.White, style = Stroke(3.5f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/* ---------------- Katakli joylashuv (grid) ---------------- */

/**
 * CSS grid kabi: [columns] ta teng ustun, qatordagi kataklar bir xil balandlikda.
 * [cell] ga katak eni va balandligini to'ldiradigan Modifier beriladi.
 */
@Composable
internal fun <T> GridRows(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalGap: Dp = 16.dp,
    verticalGap: Dp = 16.dp,
    cell: @Composable RowScope.(item: T, modifier: Modifier) -> Unit,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(verticalGap)) {
        items.chunked(columns.coerceAtLeast(1)).forEach { row ->
            /* Teng balandlik faqat bir nechta ustunda kerak. Bitta ustunda intrinsic o'lchov
               ishlatilmaydi — FlowRow balandligini kam hisoblab, karta ichini kesib qo'yardi */
            val multi = columns > 1
            Row(
                Modifier.fillMaxWidth().then(if (multi) Modifier.height(IntrinsicSize.Min) else Modifier),
                horizontalArrangement = Arrangement.spacedBy(horizontalGap),
            ) {
                row.forEach { item ->
                    cell(item, if (multi) Modifier.weight(1f).fillMaxHeight() else Modifier.weight(1f))
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/* ---------------- Tasdiqlash qatori ---------------- */

/**
 * «Barchasini tozalash» → «12 ta belgi oʻchirilsinmi? [Ha, tozalash] [Bekor qilish]».
 * Veb nusxadagidek sahifaning o'zida, oyna (dialog) ochilmaydi.
 */
@Composable
internal fun ConfirmActions(
    confirming: Boolean,
    question: String,
    triggerLabel: String,
    confirmLabel: String,
    onTrigger: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    triggerEnabled: Boolean = true,
    questionSize: TextUnit = 14.sp,
) {
    val colors = HolyTheme.colors
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (confirming) {
            Text(question, color = colors.ink, fontSize = questionSize, fontWeight = FontWeight.SemiBold)
            HolyTextButton(confirmLabel, onConfirm, variant = ButtonVariant.Danger, size = ButtonSize.Sm)
            HolyTextButton("Bekor qilish", onCancel, variant = ButtonVariant.Ghost, size = ButtonSize.Sm)
        } else {
            HolyTextButton(
                triggerLabel,
                onTrigger,
                variant = ButtonVariant.Danger,
                size = ButtonSize.Sm,
                enabled = triggerEnabled,
            )
        }
    }
}

/* ---------------- Statistika qatori ---------------- */

/** <dt>/<dd> juftligi: chapda nomi, o'ngda qiymati */
@Composable
internal fun StatRow(label: String, value: String, modifier: Modifier = Modifier, strong: Boolean = false) {
    val colors = HolyTheme.colors
    Row(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(label, color = colors.inkFaint, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            value,
            color = if (strong) colors.accent else colors.ink,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Holat nuqtasi: h-2 w-2 rounded-full */
@Composable
internal fun StatusDot(color: Color) {
    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
}

/* ---------------- Formatlash ---------------- */

/** tabular-nums: raqamlar bir xil enda — sanoq o'zgarganda matn sakramaydi */
@Composable
internal fun tabular(): TextStyle = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")

/** toLocaleString('uz'): 6236 → «6 236» (bo'linmas probel bilan) */
internal fun uzNumber(value: Long): String {
    val digits = kotlin.math.abs(value).toString()
    val grouped = digits.reversed().chunked(3).joinToString("\u00A0").reversed()
    return if (value < 0) "-$grouped" else grouped
}

internal fun uzNumber(value: Int): String = uzNumber(value.toLong())

/** 19 506 977 → «18.6 MB», 52 000 → «51 KB» */
internal fun formatSize(bytes: Long): String =
    if (bytes >= 1024L * 1024) {
        String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
    } else {
        "${maxOf(1L, Math.round(bytes / 1024.0))} KB"
    }
