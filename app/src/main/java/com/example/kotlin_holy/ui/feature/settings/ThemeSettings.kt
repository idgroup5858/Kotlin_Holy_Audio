package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.Eyebrow
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.theme.ACCENTS
import com.example.kotlin_holy.ui.theme.Appearance
import com.example.kotlin_holy.ui.theme.AppearanceBackdrop
import com.example.kotlin_holy.ui.theme.CARD_STYLES
import com.example.kotlin_holy.ui.theme.HolyColors
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.PATTERNS
import com.example.kotlin_holy.ui.theme.PATTERN_GROUPS
import com.example.kotlin_holy.ui.theme.RADII
import com.example.kotlin_holy.ui.theme.SIZES
import com.example.kotlin_holy.ui.theme.STRENGTHS
import com.example.kotlin_holy.ui.theme.THEMES
import com.example.kotlin_holy.ui.theme.ThemeDef
import com.example.kotlin_holy.ui.theme.ThemeMode
import com.example.kotlin_holy.ui.theme.accentColor
import com.example.kotlin_holy.ui.theme.holyColors

/** Yorug' / qorong'i / tizim tanlovi */
private data class ModeOption(val mode: ThemeMode, val title: String, val hint: String, val icon: ImageVector)

private val MODES = listOf(
    ModeOption(ThemeMode.LIGHT, "Yorugʻ", "Och fon, toʻq matn", HolyIcons.Sun),
    ModeOption(ThemeMode.DARK, "Qorongʻi", "Koʻzni kamroq charchatadi", HolyIcons.Moon),
    ModeOption(ThemeMode.SYSTEM, "Tizim", "Qurilma sozlamasiga qarab", HolyIcons.Monitor),
)

/* Har bir bo'lim alohida qator — uzun ro'yxat bo'lakma-bo'lak chiziladi */
private val SECTION_GAP = 20.dp

/** Sozlamalar → Mavzu (veb nusxadagi ThemeSettings.jsx) */
internal fun LazyListScope.themeSettings() {
    item(key = "theme-top") { ThemeTopLine() }
    item(key = "theme-mode") { ModeSection() }
    item(key = "theme-themes") { ThemesSection() }
    item(key = "theme-accent") { AccentSection() }
    item(key = "theme-background") { BackgroundSection() }
    item(key = "theme-cards") { CardsSection() }
}

/** Joriy tanlov va uni o'zgartirish — o'zgarish darhol butun ilovaga qo'llanadi */
@Composable
private fun rememberAppearanceUpdate(): Pair<Appearance, ((Appearance) -> Appearance) -> Unit> {
    val viewModel: SettingsViewModel = hiltViewModel()
    val appearance = HolyTheme.appearance
    return appearance to { change -> viewModel.setAppearance(change(appearance)) }
}

/* ---------------- «Sinov» qatori va standart holat ---------------- */

@Composable
private fun ThemeTopLine() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val appearance = HolyTheme.appearance
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = SECTION_GAP),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HolyBadge("Sinov", tone = BadgeTone.Accent)
            HintText("Oʻzgarishlar darhol butun ilovaga qoʻllanadi va shu qurilmada saqlanadi.")
        }
        HolyTextButton(
            "Standart holatga qaytarish",
            onClick = { viewModel.setAppearance(Appearance()) },
            enabled = !appearance.isDefault,
        )
    }
}

/* ---------------- Rejim ---------------- */

@Composable
private fun ModeSection() {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val mode = settings?.themeMode
    val colors = HolyTheme.colors

    Box(Modifier.padding(bottom = SECTION_GAP)) {
        SettingsSection(title = "Rejim", hint = "Tepadagi oy / quyosh tugmasi bilan ham almashtiriladi") {
            GridRows(MODES, columns = columnsFor(1, sm = 3), horizontalGap = 12.dp, verticalGap = 12.dp) { item, modifier ->
                val selected = mode == item.mode
                OptionTile(
                    selected = selected,
                    onClick = { viewModel.setThemeMode(item.mode) },
                    modifier = modifier,
                    contentPadding = PaddingValues(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.accent else colors.surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = null,
                                tint = if (selected) Color.White else colors.inkSoft,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column {
                            Text(item.title, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(item.hint, color = colors.inkFaint, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}

/* ---------------- Mavzu ---------------- */

@Composable
private fun ThemesSection() {
    val (appearance, update) = rememberAppearanceUpdate()
    val colors = HolyTheme.colors
    val dark = colors.isDark
    val active = appearance.themeDef

    Box(Modifier.padding(bottom = SECTION_GAP)) {
        SettingsSection(
            title = "Mavzu",
            hint = "Fon, kartochkalar, matn va asosiy rang — butun ilova ranglari",
            aside = { AsideLabel(active.name) },
        ) {
            GridRows(THEMES, columns = columnsFor(2, sm = 3, lg = 5)) { theme, modifier ->
                OptionTile(
                    selected = active.id == theme.id,
                    onClick = { update { it.copy(theme = theme.id) } },
                    modifier = modifier,
                    label = theme.name,
                ) {
                    Column {
                        val preview = remember(appearance.accent, theme.id, dark) {
                            holyColors(appearance.copy(theme = theme.id), dark)
                        }
                        ThemePreview(preview)
                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp, start = 2.dp, end = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    theme.name,
                                    color = colors.ink,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    theme.hint,
                                    color = colors.inkFaint,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            ThemeDots(theme)
                        }
                    }
                }
            }
        }
    }
}

/** Mavzuning kichik namunasi: yon panel, sarlavha, ikki kartochka va tugma — o'z ranglarida */
@Composable
private fun ThemePreview(c: HolyColors) {
    val shape = HolyTheme.shape
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bg)
            .border(1.dp, c.line, shape)
            .padding(1.dp),
    ) {
        Row(Modifier.fillMaxWidth().height(76.dp)) {
            Column(
                Modifier
                    .width(24.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val stroke = 1.dp.toPx()
                        drawRect(c.line, Offset(size.width - stroke, 0f), androidx.compose.ui.geometry.Size(stroke, size.height))
                    }
                    .padding(top = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Pill(12.dp, 6.dp, c.accent)
                Pill(12.dp, 4.dp, c.inkFaint.copy(alpha = 0.4f))
                Pill(12.dp, 4.dp, c.inkFaint.copy(alpha = 0.4f))
            }
            Column(Modifier.weight(1f).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(48.dp, 6.dp, c.ink)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniCard(c, 2f / 3f, Modifier.weight(1f))
                    MiniCard(c, 1f / 2f, Modifier.weight(1f))
                }
                Pill(36.dp, 8.dp, c.accent)
            }
        }
    }
}

@Composable
private fun Pill(width: Dp, height: Dp, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(width, height).clip(CircleShape).background(color))
}

@Composable
private fun MiniCard(c: HolyColors, secondLine: Float, modifier: Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Column(
        modifier
            .height(28.dp)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.line, shape)
            .padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(c.inkSoft.copy(alpha = 0.6f)))
        Box(Modifier.fillMaxWidth(secondLine).height(4.dp).clip(CircleShape).background(c.inkFaint.copy(alpha = 0.5f)))
    }
}

/** Yorug' va qorong'i variantlari — ikki doiracha bir-birining ustiga chiqib turadi */
@Composable
private fun ThemeDots(theme: ThemeDef) {
    val surface = HolyTheme.colors.surface
    Box(Modifier.size(width = 24.dp, height = 14.dp)) {
        listOf(theme.light.bg to 0.dp, theme.dark.bg to 10.dp).forEach { (color, x) ->
            Box(
                Modifier
                    .offset(x = x)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(2.dp, surface, CircleShape),
            )
        }
    }
}

/* ---------------- Asosiy rang ---------------- */

@Composable
private fun AccentSection() {
    val (appearance, update) = rememberAppearanceUpdate()
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val dark = colors.isDark
    val active = appearance.accentDef

    Box(Modifier.padding(bottom = SECTION_GAP)) {
        SettingsSection(
            title = "Asosiy rang",
            hint = "Tugmalar, havolalar, belgilar va faol boʻlim rangi — mavzudagidan boshqasini tanlash mumkin",
            aside = { AsideLabel(active.name) },
        ) {
            GridRows(ACCENTS, columns = columnsFor(4, sm = 8), horizontalGap = 8.dp, verticalGap = 8.dp) { accent, modifier ->
                val selected = active.id == accent.id
                val color = accentColor(appearance, accent.id, dark)
                Column(
                    modifier
                        .clip(shape)
                        .background(if (selected) colors.accent.copy(alpha = 0.08f) else Color.Transparent)
                        .border(1.dp, if (selected) colors.accent else Color.Transparent, shape)
                        .clickable { update { it.copy(accent = accent.id) } }
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val ringColor = colors.line
                    val offsetColor = colors.surface
                    Box(
                        Modifier
                            .size(32.dp)
                            .then(
                                /* «Mavzudagi» — ring-2 ring-offset-2: rang mavzunikiligini bildiradi */
                                if (accent.id == "theme") {
                                    Modifier.drawBehind {
                                        drawCircle(ringColor, size.minDimension / 2 + 4.dp.toPx())
                                        drawCircle(offsetColor, size.minDimension / 2 + 2.dp.toPx())
                                    }
                                } else {
                                    Modifier
                                },
                            )
                            .clip(CircleShape)
                            .background(color),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            Icon(HolyIcons.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        accent.name,
                        color = if (selected) colors.accent else colors.inkSoft,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/* ---------------- Orqa fon ---------------- */

@Composable
private fun BackgroundSection() {
    val (appearance, update) = rememberAppearanceUpdate()

    Box(Modifier.padding(bottom = SECTION_GAP)) {
        SettingsSection(
            title = "Orqa fon",
            hint = "Sahifa ortidagi naqsh — ${PATTERNS.size - 1} xil, rangi mavzuga moslashadi",
            aside = { AsideLabel(appearance.patternDef.name) },
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Control("Kuchi") {
                    ScrollableSegmented(
                        STRENGTHS.map { it.value to it.label },
                        appearance.strength,
                        { value -> update { it.copy(strength = value) } },
                    )
                }
                Control("Oʻlchami") {
                    ScrollableSegmented(
                        SIZES.map { it.value to it.label },
                        appearance.size,
                        { value -> update { it.copy(size = value) } },
                    )
                }
                Toggle(appearance.tint, { value -> update { it.copy(tint = value) } }, "Asosiy rangda")
                Toggle(appearance.glow, { value -> update { it.copy(glow = value) } }, "Rangli yogʻdu")
            }
            Divider()
            Spacer(Modifier.height(24.dp))

            val activePattern = appearance.patternDef.id
            val columns = columnsFor(2, sm = 4, lg = 6, xl = 8)
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                PATTERN_GROUPS.forEach { group ->
                    val patterns = PATTERNS.filter { it.group == group.id }
                    Column {
                        Eyebrow("${group.title} · ${patterns.size}", Modifier.padding(bottom = 12.dp))
                        GridRows(patterns, columns = columns) { pattern, modifier ->
                            OptionTile(
                                selected = activePattern == pattern.id,
                                onClick = { update { it.copy(pattern = pattern.id) } },
                                modifier = modifier,
                                label = pattern.name,
                            ) {
                                Column {
                                    BackgroundPreview(pattern.id, appearance)
                                    Text(
                                        pattern.name,
                                        color = HolyTheme.colors.ink,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Nomli boshqaruv: «Kuchi: [Past | O'rta | Kuchli]» */
@Composable
private fun Control(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = HolyTheme.colors.inkFaint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

/** role="switch" — h-5 w-9 yo'lak, oq tutqich */
@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, label: String) {
    val colors = HolyTheme.colors
    val knob by animateDpAsState(if (checked) 18.dp else 2.dp, tween(150), label = "toggle")
    Row(
        Modifier
            .clip(HolyTheme.shape)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(width = 36.dp, height = 20.dp)
                .clip(CircleShape)
                .background(if (checked) colors.accent else colors.surface2)
                .border(1.dp, if (checked) Color.Transparent else colors.line, CircleShape),
        ) {
            Box(
                Modifier
                    .offset(x = knob, y = 2.dp)
                    .size(16.dp)
                    .shadow(1.5.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        Text(label, color = colors.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Orqa fon namunasi — joriy mavzu ranglarida, tanlangan kuch, o'lcham va rangda */
@Composable
private fun BackgroundPreview(pattern: String, appearance: Appearance) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Box(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(colors.bg)
            .border(1.dp, colors.line, shape),
    ) {
        AppearanceBackdrop(
            appearance,
            colors,
            Modifier.matchParentSize(),
            pattern = pattern,
            showGlow = appearance.glow,
        )
    }
}

/* ---------------- Kartochkalar ---------------- */

@Composable
private fun CardsSection() {
    val (appearance, update) = rememberAppearanceUpdate()
    val colors = HolyTheme.colors

    Box(Modifier.padding(bottom = SECTION_GAP)) {
        SettingsSection(
            title = "Kartochkalar",
            hint = "Suralar, sahifalar, hadislar va sozlamalardagi bloklar koʻrinishi",
            aside = {
                ScrollableSegmented(
                    RADII.map { it.value to it.label },
                    appearance.radius,
                    { value -> update { it.copy(radius = value) } },
                )
            },
        ) {
            GridRows(CARD_STYLES, columns = columnsFor(2, lg = 4)) { style, modifier ->
                OptionTile(
                    selected = appearance.cardStyle == style.id,
                    onClick = { update { it.copy(cards = style.id) } },
                    modifier = modifier,
                    label = style.name,
                ) {
                    Column {
                        CardPreview(style.id, appearance)
                        Column(Modifier.padding(top = 8.dp, start = 2.dp, end = 2.dp)) {
                            Text(style.name, color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(style.hint, color = colors.inkFaint, fontSize = 11.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
            HintText(
                "Oʻngdagi tanlov — burchaklar: tugmalar, kartochkalar va maydonlar hammasi birga oʻzgaradi.",
                modifier = Modifier.padding(top = 16.dp),
                fontSize = 12.sp,
            )
        }
    }
}

/** Kartochka uslubi namunasi: naqshli fon ustida bitta kartochka */
@Composable
private fun CardPreview(style: String, appearance: Appearance) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val dark = colors.isDark
    val surface = if (style == "glass") colors.surface.copy(alpha = if (dark) 0.68f else 0.72f) else colors.surface
    val border = if (style == "borderless") Color.Transparent else colors.line
    val elevation = when {
        style == "shadow" -> 6.dp
        style == "borderless" && !dark -> 1.5.dp
        else -> 0.dp
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(shape)
            .background(colors.bg)
            .border(1.dp, colors.line, shape),
    ) {
        AppearanceBackdrop(appearance, colors, Modifier.matchParentSize(), pattern = "lines", showGlow = false)
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .then(
                    if (elevation > 0.dp) {
                        Modifier.shadow(
                            elevation = elevation,
                            shape = shape,
                            ambientColor = Color.Black.copy(alpha = if (dark) 0.4f else 0.08f),
                            spotColor = Color.Black.copy(alpha = if (dark) 0.6f else 0.22f),
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .background(surface)
                .border(1.dp, border, shape)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.fillMaxWidth(0.75f).height(6.dp).clip(CircleShape).background(colors.inkSoft.copy(alpha = 0.6f)))
            Box(Modifier.fillMaxWidth(0.5f).height(6.dp).clip(CircleShape).background(colors.inkFaint.copy(alpha = 0.5f)))
        }
    }
}
