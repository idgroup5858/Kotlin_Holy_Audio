package com.example.kotlin_holy.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.theme.HolyTheme

/** Veb nusxadagi Card: yumshoq burchak, nozik hoshiya */
@Composable
fun HolyCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    background: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val border by animateColorAsState(borderColor ?: colors.line, label = "card-border")
    Box(
        modifier = modifier
            .clip(shape)
            .background(background ?: colors.surface)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        content()
    }
}

/**
 * Ixcham qidiruv/raqam maydoni: standart OutlinedTextField'dan farqli
 * o'laroq, balandligi matn hajmiga qarab o'zi kichrayadi — 56dp'lik qattiq
 * minimal balandlikka bog'lanib qolmaydi, shuning uchun kesilib qolmaydi.
 * Burchak radiusi kartalar bilan bir xil (16dp).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    val colors = HolyTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(16.dp)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.line,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        cursorColor = colors.accent,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        textStyle = LocalTextStyle.current.copy(color = colors.ink, fontSize = 14.sp),
        cursorBrush = SolidColor(colors.accent),
        interactionSource = interactionSource,
    ) { innerTextField ->
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = true,
            singleLine = singleLine,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            placeholder = placeholder?.let { text ->
                { Text(text, color = colors.inkFaint, fontSize = 14.sp) }
            },
            leadingIcon = leadingIcon,
            colors = fieldColors,
            contentPadding = OutlinedTextFieldDefaults.contentPadding(
                start = 14.dp,
                end = 14.dp,
                top = 12.dp,
                bottom = 12.dp,
            ),
            container = {
                OutlinedTextFieldDefaults.Container(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = fieldColors,
                    shape = shape,
                )
            },
        )
    }
}

/** Kichik yorliq: "Makkiy", "286 / 286 oyat" */
@Composable
fun HolyBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    background: Color? = null,
) {
    val colors = HolyTheme.colors
    Text(
        text = text,
        color = color ?: colors.inkSoft,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(CircleShape)
            .background(background ?: colors.surface2)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** Ekran sarlavhasi: katta nom va ostida izoh */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = HolyTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    color = colors.inkFaint,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** Bo'lim sarlavhasi (sozlamalardagi kartochkalar ichida) */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = HolyTheme.colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        if (trailing != null) trailing()
    }
}

/** Ma'lumot topilmaganda ko'rsatiladigan holat */
@Composable
fun EmptyState(title: String, hint: String? = null, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("۩", color = colors.inkFaint, fontSize = 34.sp)
        Text(
            title,
            color = colors.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (hint != null) {
            Text(
                hint,
                color = colors.inkFaint,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

/** Yuklanayotganda ko'rinadigan bo'sh qatorlar */
@Composable
fun LoadingRows(rows: Int = 4, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(rows) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(HolyTheme.colors.surface2),
            )
        }
    }
}

/** «Oyat bo'yicha | Sahifa bo'yicha» kabi almashtirgich */
@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(colors.surface2)
            .border(1.dp, colors.line, CircleShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (key, label) ->
            val active = key == value
            Text(
                text = label,
                color = if (active) colors.bg else colors.inkSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable { onChange(key) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

/** Sahifalar orasida yurish: «Oldingi / Keyingi» */
@Composable
fun ReaderPager(
    label: String?,
    onPrev: (() -> Unit)?,
    onNext: (() -> Unit)?,
    prevLabel: String = "Oldingi",
    nextLabel: String = "Keyingi",
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        PagerButton(prevLabel, enabled = onPrev != null, onClick = { onPrev?.invoke() })
        if (label != null) {
            Text(label, color = colors.inkFaint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        PagerButton(nextLabel, enabled = onNext != null, onClick = { onNext?.invoke() })
    }
}

@Composable
private fun PagerButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = HolyTheme.colors
    Text(
        text = text,
        color = if (enabled) colors.inkSoft else colors.inkFaint.copy(alpha = 0.4f),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.surface2)
            .border(1.dp, colors.line, CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** Doira ichidagi ikonka tugma (sarlavhalardagi qidiruv, sozlama va h.k.) */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = HolyTheme.colors
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.inkSoft,
            modifier = Modifier.size(20.dp),
        )
    }
}
