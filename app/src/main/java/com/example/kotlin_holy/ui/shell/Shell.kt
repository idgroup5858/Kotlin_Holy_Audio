package com.example.kotlin_holy.ui.shell

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.navigation.BackTarget
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.theme.AmiriQuran
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ThemeMode

/** Joriy yo'l: naqsh (Routes.*) va qiymatlari — navbar va QuranNav uchun */
@Immutable
data class RouteInfo(val pattern: String? = null, val args: Map<String, String> = emptyMap()) {
    fun int(name: String): Int? = args[name]?.toIntOrNull()

    /** Yo'lning to'ldirilgan ko'rinishi — «Davom etish» bilan solishtirish uchun */
    val filled: String?
        get() = pattern?.let { p ->
            var result = p.substringBefore('?')
            args.forEach { (key, value) -> result = result.replace("{$key}", value) }
            if (p == Routes.SURAH && args["ayah"] != null) result += "?ayah=${args["ayah"]}"
            result
        }
}

/** Ilova qobig'i ma'lumotlari: sura ro'yxati va joriy yo'l */
@Immutable
data class ShellInfo(
    val surahs: List<Surah> = emptyList(),
    val route: RouteInfo = RouteInfo(),
    /** Keng ekran (planshet) — QuranNav navbarda, tor ekranda sahifa boshida */
    val wide: Boolean = false,
)

val LocalShell = staticCompositionLocalOf { ShellInfo() }

/* ---------------- Navbar ---------------- */

/**
 * Tepadagi navbar (h-16): menyu, belgi, «‹ Orqaga», o'ng tomonda — o'qish vaqti va xatm
 * (keng ekranda), «Davom etish», yorug'/qorong'i almashtirgich va Sozlamalar.
 */
@Composable
fun AppHeader(
    showMenu: Boolean,
    onMenu: () -> Unit,
    onLogo: () -> Unit,
    back: BackTarget?,
    onBack: (String) -> Unit,
    lastReadLabel: String?,
    showContinue: Boolean,
    onContinue: () -> Unit,
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    settingsActive: Boolean,
    onSettings: () -> Unit,
    xatm: com.example.kotlin_holy.ui.XatmSummary,
    todayReading: Long,
    onXatm: () -> Unit,
    onReading: () -> Unit,
    quranNav: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.bg.copy(alpha = 0.92f))
            .statusBarsPadding(),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(64.dp)) {
            val width = maxWidth
            val compact = width < 520.dp
            val showNavInHeader = width >= 640.dp
            Row(
                Modifier.fillMaxWidth().fillMaxHeight().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (showMenu) {
                    HeaderIconButton(HolyIcons.Menu, "Menyu", onMenu)
                }

                Row(
                    Modifier.clip(HolyTheme.shape).clickable(onClick = onLogo),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(36.dp).clip(HolyTheme.shape).background(colors.accent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("ق", color = Color.White, fontFamily = AmiriQuran, fontSize = 16.sp)
                    }
                    if (!compact || back == null) {
                        Column {
                            Text(
                                "Qur'on",
                                color = colors.ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.3).sp,
                                lineHeight = 18.sp,
                            )
                            if (!compact) {
                                Text(
                                    "O'ZBEKCHA TARJIMA",
                                    color = colors.inkFaint,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                    lineHeight = 12.sp,
                                )
                            }
                        }
                    }
                }

                if (showNavInHeader) quranNav()

                if (back != null) {
                    if (!compact) Box(Modifier.width(1.dp).height(20.dp).background(colors.line))
                    Row(
                        Modifier
                            .height(32.dp)
                            .clip(HolyTheme.shape)
                            .clickable { onBack(back.route) }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(HolyIcons.ChevronLeft, back.label, tint = colors.inkSoft, modifier = Modifier.size(16.dp))
                        if (!compact) {
                            Text(
                                back.label,
                                color = colors.inkSoft,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (width >= 1120.dp) ReadingTimeBadge(todayReading, onReading)
                    if (width >= 1024.dp) XatmBadge(xatm, onXatm)
                    if (showContinue && lastReadLabel != null) {
                        ContinueReadingButton(
                            label = lastReadLabel,
                            showPrefix = !compact,
                            onClick = onContinue,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    ThemeSwitch(themeMode, onThemeMode)
                    HeaderIconButton(
                        HolyIcons.Settings,
                        "Sozlamalar",
                        onSettings,
                        active = settingsActive,
                        iconSize = 18,
                    )
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    active: Boolean = false,
    iconSize: Int = 20,
) {
    val colors = HolyTheme.colors
    Box(
        Modifier
            .size(32.dp)
            .clip(HolyTheme.shape)
            .background(if (active) colors.surface2 else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (active) colors.accent else colors.inkSoft, modifier = Modifier.size(iconSize.dp))
    }
}

/** Yorug' / qorong'i / tizim — header'dagi almashtirgich va Sozlamalar bir xil holatni ko'radi */
@Composable
fun ThemeSwitch(mode: ThemeMode, onChange: (ThemeMode) -> Unit) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Row(
        Modifier
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, colors.line, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(
            Triple(ThemeMode.LIGHT, HolyIcons.Sun, "Yorugʻ"),
            Triple(ThemeMode.DARK, HolyIcons.Moon, "Qorongʻi"),
            Triple(ThemeMode.SYSTEM, HolyIcons.Monitor, "Tizim"),
        ).forEach { (value, icon, title) ->
            val active = mode == value
            Box(
                Modifier
                    .size(24.dp)
                    .then(if (active) Modifier.shadow(1.dp, shape) else Modifier)
                    .clip(shape)
                    .background(if (active) colors.surface else Color.Transparent)
                    .clickable { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, title, tint = if (active) colors.ink else colors.inkFaint, modifier = Modifier.size(15.dp))
            }
        }
    }
}

/** «Davom etish: Baqara 2:21» — oxirgi o'qilgan joyga bir bosishda qaytish */
@Composable
private fun ContinueReadingButton(
    label: String,
    showPrefix: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Row(
        modifier
            .height(32.dp)
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, colors.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(HolyIcons.Bookmark, "Oxirgi oʻqilgan joydan davom etish", tint = colors.accent, modifier = Modifier.size(16.dp))
        if (showPrefix) {
            Text("Davom etish:", color = colors.inkSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Text(
            label,
            color = colors.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 140.dp),
        )
    }
}

/** Navbardagi bugungi o'qish vaqti — keng ekranda */
@Composable
private fun ReadingTimeBadge(today: Long, onClick: () -> Unit) {
    val colors = HolyTheme.colors
    Row(
        Modifier.height(48.dp).clip(HolyTheme.shape).clickable(onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(HolyIcons.Clock, null, tint = colors.read, modifier = Modifier.size(18.dp))
        Column {
            Text(ReadingTracker.formatDuration(today, short = true), color = colors.ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("bugun oʻqildi", color = colors.inkFaint, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Navbardagi xatm holati — son va doira (keng ekranda) */
@Composable
private fun XatmBadge(xatm: com.example.kotlin_holy.ui.XatmSummary, onClick: () -> Unit) {
    val colors = HolyTheme.colors
    val count = xatm.pages.size
    val ratio = (count / 604f).coerceAtMost(1f)
    val percent = Math.round(ratio * 1000) / 10.0
    Row(
        Modifier.height(48.dp).clip(HolyTheme.shape).clickable(onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("$count", color = colors.read, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 24.sp)
            Text("/ 604", color = colors.inkFaint, fontSize = 12.sp)
        }
        XatmRing(ratio, percentLabel(percent))
    }
}

fun percentLabel(percent: Double): String =
    if (percent == Math.floor(percent)) percent.toInt().toString() else percent.toString()

/** Doira shaklidagi progress — markazida foiz */
@Composable
fun XatmRing(ratio: Float, label: String) {
    val colors = HolyTheme.colors
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(44.dp)) {
            val stroke = 4.5.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(colors.line, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            drawArc(
                colors.read, -90f, 360f * ratio, false, Offset(inset, inset), arcSize,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        Text("$label%", color = colors.ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

/* ---------------- Yon panel ---------------- */

private data class NavItem(val route: String, val label: String, val icon: ImageVector, val match: (String?) -> Boolean)

private val GROUPS = listOf(
    "O'qish" to listOf(
        NavItem(Routes.HOME, "Suralar", HolyIcons.Surahs) { it == Routes.HOME || it?.startsWith("surah/") == true },
        NavItem(Routes.PAGES, "Sahifalar", HolyIcons.Pages) { it == Routes.PAGES || it == Routes.PAGE },
        NavItem(Routes.JUZ_LIST, "Juzlar", HolyIcons.Juz) { it?.startsWith("juz") == true },
    ),
    "Kutubxona" to listOf(
        NavItem(Routes.BOOKS, "Hadislar", HolyIcons.Hadith) { it == Routes.BOOKS || it?.startsWith("book/") == true },
        NavItem(Routes.LIBRARY, "Kitoblar", HolyIcons.Library) { it?.startsWith("library") == true },
    ),
    "Lug'at" to listOf(
        NavItem(Routes.WORDS, "Qiyin so'zlar", HolyIcons.Words) { it == Routes.WORDS },
    ),
)

/** Yon panel: O'qish · Kutubxona · Lug'at */
@Composable
fun Sidebar(currentRoute: String?, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    Column(
        modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        GROUPS.forEach { (title, items) ->
            Column {
                Text(
                    title.uppercase(),
                    color = colors.inkFaint,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.3.sp,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items.forEach { item ->
                        val active = item.match(currentRoute)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(HolyTheme.shape)
                                .background(if (active) colors.surface2 else Color.Transparent)
                                .clickable { onNavigate(item.route) },
                        ) {
                            if (active) {
                                Box(
                                    Modifier
                                        .align(Alignment.CenterStart)
                                        .width(3.dp)
                                        .height(20.dp)
                                        .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                                        .background(colors.accent),
                                )
                            }
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(item.icon, null, tint = if (active) colors.accent else colors.inkSoft, modifier = Modifier.size(18.dp))
                                Text(
                                    item.label,
                                    color = if (active) colors.ink else colors.inkSoft,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
