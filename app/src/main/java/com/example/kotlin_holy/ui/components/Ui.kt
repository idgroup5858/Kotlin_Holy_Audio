package com.example.kotlin_holy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.JakartaSans

/* ---------------- Surface ---------------- */

/**
 * Veb nusxadagi Card: bg-surface, border-line, rounded-md. Ko'rinishi Sozlamalar → Mavzu →
 * Kartochkalar tanloviga bog'liq (tekis, soyali, shaffof, chegarasiz).
 * [borderColor] / [background] berilsa, uslubdan ustun turadi (masalan o'qilgan sura — yashil).
 */
@Composable
fun HolyCard(
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    background: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    val style = HolyTheme.appearance.cardStyle
    val shape = HolyTheme.shape
    val surface = when (style) {
        "glass" -> colors.surface.copy(alpha = if (colors.isDark) 0.68f else 0.72f)
        else -> colors.surface
    }
    val border = borderColor ?: if (style == "borderless") Color.Transparent else colors.line
    val elevated = style == "shadow" || (style == "borderless" && !colors.isDark)

    Box(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        elevation = if (style == "shadow") 6.dp else 1.5.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = if (colors.isDark) 0.4f else 0.08f),
                        spotColor = Color.Black.copy(alpha = if (colors.isDark) 0.6f else 0.22f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(shape)
            .background(background ?: surface)
            .border(1.dp, border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        content()
    }
}

/** Karta ichidagi bo'lim sarlavhasi: text-sm font-semibold */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text, color = HolyTheme.colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        trailing?.invoke(this)
    }
}

/** Sahifa sarlavhasi: text-3xl va ostida izoh */
@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = HolyTheme.colors
    Column(modifier.fillMaxWidth().padding(bottom = 28.dp)) {
        Text(title, color = colors.ink, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp)
        if (subtitle != null) {
            Text(
                subtitle,
                color = colors.inkFaint,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
        if (trailing != null) Box(Modifier.padding(top = 16.dp)) { trailing() }
    }
}

/* ---------------- Button ---------------- */

enum class ButtonVariant { Primary, Secondary, Ghost, Danger }
enum class ButtonSize { Sm, Md, Icon }

@Composable
fun HolyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Secondary,
    size: ButtonSize = ButtonSize.Md,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val (bg, fg) = when (variant) {
        ButtonVariant.Primary -> colors.accent to Color.White
        ButtonVariant.Secondary -> colors.surface2 to colors.inkSoft
        ButtonVariant.Ghost -> Color.Transparent to colors.inkSoft
        ButtonVariant.Danger -> Color.Transparent to Color(0xFFEF4444)
    }
    val height = when (size) {
        ButtonSize.Sm -> 30.dp
        ButtonSize.Md -> 34.dp
        ButtonSize.Icon -> 34.dp
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides fg,
    ) {
        Row(
            modifier = modifier
                .height(height)
                .then(if (size == ButtonSize.Icon) Modifier.width(height) else Modifier)
                .clip(shape)
                .background(bg)
                .then(
                    if (variant == ButtonVariant.Secondary) Modifier.border(1.dp, colors.line, shape) else Modifier,
                )
                .clickable(enabled = enabled, onClick = onClick)
                .then(if (!enabled) Modifier.alpha(0.5f) else Modifier)
                .padding(horizontal = if (size == ButtonSize.Icon) 0.dp else if (size == ButtonSize.Sm) 10.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            content = content,
        )
    }
}

/** Tugma matni — rangini tugma o'zi beradi */
@Composable
fun ButtonText(text: String, size: ButtonSize = ButtonSize.Md) {
    Text(
        text,
        color = androidx.compose.material3.LocalContentColor.current,
        fontSize = if (size == ButtonSize.Sm) 12.sp else 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

/** Matnli oddiy tugma */
@Composable
fun HolyTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Secondary,
    size: ButtonSize = ButtonSize.Md,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    HolyButton(onClick, modifier, variant, size, enabled) {
        if (icon != null) {
            Icon(icon, null, tint = androidx.compose.material3.LocalContentColor.current, modifier = Modifier.size(15.dp))
        }
        ButtonText(text, size)
    }
}

/* ---------------- Segmented / Tabs ---------------- */

/** «Arabcha | Tarjima» kabi almashtirgich: faol tugma bg-ink text-bg */
@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, colors.line, shape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (key, label) ->
            val active = key == value
            Text(
                text = label,
                color = if (active) colors.bg else colors.inkSoft,
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier
                    .clip(shape)
                    .background(if (active) colors.ink else Color.Transparent)
                    .clickable { onChange(key) }
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

/** Bo'lim tablari — Sozlamalar va bosh sahifada bir xil; tor ekranda gorizontal suriladi */
@Composable
fun HolyTabs(
    tabs: List<Pair<String, String>>,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.horizontalScroll(rememberScrollState())) {
        Segmented(options = tabs, value = value, onChange = onChange, fontSize = 13.sp)
    }
}

/** Katak / ro'yxat ko'rinishini almashtirgich */
@Composable
fun ViewToggle(view: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Row(
        modifier = modifier
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, colors.line, shape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf("grid" to HolyIcons.Grid, "list" to HolyIcons.ListView).forEach { (key, icon) ->
            val active = view == key
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(if (active) colors.accent.copy(alpha = 0.15f) else Color.Transparent)
                    .border(1.dp, if (active) colors.accent.copy(alpha = 0.5f) else Color.Transparent, shape)
                    .clickable { onChange(key) }
                    .padding(6.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = if (key == "grid") "Katak koʻrinish" else "Roʻyxat koʻrinish",
                    tint = if (active) colors.accent else colors.inkFaint,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/* ---------------- Form controls ---------------- */

/** Veb nusxadagi Input: bg-surface-2, border-line, fokusda asosiy rangli hoshiya */
@Composable
fun HolyInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    minHeight: Dp = 36.dp,
    textStyle: TextStyle? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style = (textStyle ?: TextStyle(fontSize = 14.sp, fontFamily = JakartaSans)).copy(color = colors.ink)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        interactionSource = interaction,
        keyboardOptions = keyboardOptions,
        modifier = modifier
            .heightIn(min = minHeight)
            .then(if (focused) Modifier.border(4.dp, colors.accent.copy(alpha = 0.15f), shape) else Modifier)
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, if (focused) colors.accent else colors.line, shape),
        decorationBox = { inner ->
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = if (singleLine) 8.dp else 10.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
            ) {
                if (leadingIcon != null) {
                    Icon(leadingIcon, null, tint = colors.inkFaint, modifier = Modifier.padding(end = 10.dp).size(16.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty() && placeholder != null) {
                        Text(placeholder, style = style.copy(color = colors.inkFaint), maxLines = if (singleLine) 1 else 3)
                    }
                    inner()
                }
            }
        },
    )
}

/** Yorliqli maydon: Field label + izoh */
@Composable
fun FormField(label: String, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val colors = HolyTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = colors.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        content()
        if (hint != null) Text(hint, color = colors.inkFaint, fontSize = 12.sp)
    }
}

/* ---------------- Feedback ---------------- */

enum class BadgeTone { Neutral, Accent, Read }

@Composable
fun HolyBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: BadgeTone = BadgeTone.Neutral,
    onClick: (() -> Unit)? = null,
) {
    val colors = HolyTheme.colors
    val (bg, fg) = when (tone) {
        BadgeTone.Neutral -> colors.surface2 to colors.inkSoft
        BadgeTone.Accent -> colors.accent.copy(alpha = 0.12f) to colors.accent
        BadgeTone.Read -> colors.read.copy(alpha = 0.10f) to colors.read
    }
    Text(
        text = text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(HolyTheme.shape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

@Composable
fun HolyProgress(value: Int, total: Int, modifier: Modifier = Modifier, color: Color? = null) {
    val colors = HolyTheme.colors
    val target = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else 0f
    val ratio by animateFloatAsState(target, tween(500), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(colors.surface2),
    ) {
        Box(
            Modifier
                .fillMaxWidth(ratio)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color ?: colors.accent),
        )
    }
}

@Composable
fun AlertBox(text: String?, modifier: Modifier = Modifier, error: Boolean = true) {
    if (text.isNullOrBlank()) return
    val colors = HolyTheme.colors
    val tint = if (error) Color(0xFFEF4444) else colors.accent
    Text(
        text,
        color = tint,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier
            .fillMaxWidth()
            .clip(HolyTheme.shape)
            .background(tint.copy(alpha = if (error) 0.10f else 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** Yuklanayotgan joy: bg-surface-2 animate-pulse */
@Composable
fun Skeleton(modifier: Modifier = Modifier, shape: Shape? = null) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    Box(
        modifier
            .clip(shape ?: HolyTheme.shape)
            .background(HolyTheme.colors.surface2.copy(alpha = alpha)),
    )
}

@Composable
fun LoadingRows(rows: Int = 3, modifier: Modifier = Modifier, height: Dp = 80.dp) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) { Skeleton(Modifier.fillMaxWidth().height(height)) }
    }
}

/** Ma'lumot yo'q holati: uzuq chiziqli hoshiya, ۞ belgisi */
@Composable
fun EmptyState(
    title: String,
    hint: String? = null,
    modifier: Modifier = Modifier,
    icon: String = "۞",
    action: (@Composable () -> Unit)? = null,
) {
    val colors = HolyTheme.colors
    val radius = HolyTheme.radius
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = colors.line,
                    cornerRadius = CornerRadius(radius.toPx()),
                    style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
                )
            }
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(44.dp).clip(HolyTheme.shape).background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, color = colors.inkFaint, fontSize = 18.sp)
        }
        Text(
            title,
            color = colors.ink,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        if (hint != null) {
            Text(
                hint,
                color = colors.inkFaint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp).widthIn(max = 384.dp),
            )
        }
        if (action != null) Box(Modifier.padding(top = 16.dp)) { action() }
    }
}

/** Ochiladigan blok: balandligi silliq o'zgaradi */
@Composable
fun Collapse(open: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = open,
        enter = expandVertically(tween(200)) + fadeIn(tween(200)),
        exit = shrinkVertically(tween(200)) + fadeOut(tween(200)),
    ) {
        content()
    }
}

/** Sura raqami uchun romb belgisi */
@Composable
fun Diamond(text: String, modifier: Modifier = Modifier, background: Color? = null) {
    val colors = HolyTheme.colors
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(31.dp)
                .rotate(45f)
                .clip(HolyTheme.shape)
                .background(background ?: colors.surface2),
        )
        Text(text, color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

/** Yashil doiradagi belgi — o'qildi tugmalari va tanlangan katak uchun */
@Composable
fun CheckCircle(
    checked: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    color: Color? = null,
    partial: Boolean = false,
    contentDescription: String? = null,
) {
    val colors = HolyTheme.colors
    val tint = color ?: colors.read
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (checked) tint else Color.Transparent)
            .border(
                1.dp,
                when {
                    checked -> tint
                    partial -> tint.copy(alpha = 0.6f)
                    else -> colors.line
                },
                CircleShape,
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            HolyIcons.Check,
            contentDescription = contentDescription,
            tint = when {
                checked -> Color.White
                partial -> tint.copy(alpha = 0.6f)
                else -> Color.Transparent
            },
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

/** Sarlavhadagi kichik bosh harfli yorliq: «O'QISH», «ODDIY · 12» */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = HolyTheme.colors.inkFaint,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp,
        modifier = modifier,
    )
}

/** Ingichka ajratuvchi chiziq */
@Composable
fun Divider(modifier: Modifier = Modifier, color: Color? = null) {
    Box(modifier.fillMaxWidth().height(1.dp).background(color ?: HolyTheme.colors.line))
}

/** Tik ajratuvchi chiziq */
@Composable
fun VerticalDivider(modifier: Modifier = Modifier) {
    Box(modifier.width(1.dp).height(IntrinsicSize.Min).background(HolyTheme.colors.line))
}

/** Bo'sh joy */
@Composable
fun Gap(height: Dp) = Spacer(Modifier.height(height))

/* ---------------- Jadval uslubidagi ro'yxat ---------------- */

/** LIST_BOX: border-line rounded-md, ichida zebra qatorlar */
@Composable
fun ListBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = HolyTheme.shape
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(BorderStroke(1.dp, HolyTheme.colors.line), shape),
        content = content,
    )
}

/** Zebra qator foni: juft — surface, toq — surface-2 */
@Composable
fun rowBackground(index: Int): Color =
    if (index % 2 == 1) HolyTheme.colors.surface2 else HolyTheme.colors.surface

/** «Sahifa/oyat» ro'yxati yuklanayotganda */
@Composable
fun ListSkeleton(rows: Int = 9, rowHeight: Dp = 52.dp) {
    ListBox {
        repeat(rows) { index ->
            Skeleton(Modifier.fillMaxWidth().height(rowHeight), shape = RoundedCornerShape(0.dp))
            if (index < rows - 1) Divider()
        }
    }
}
