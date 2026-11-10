package com.example.kotlin_holy.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.HolyInput
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.theme.AmiriQuran
import com.example.kotlin_holy.ui.theme.HolyTheme

private val TABS = listOf("surah" to "Sura", "verse" to "Oyat", "juz" to "Juz", "page" to "Sahifa")

/** Tugmadagi yozuv: joriy sura / sahifa / juz yoki «Qur'on bo'ylab o'tish» */
private fun currentLabel(route: RouteInfo, surahs: List<Surah>): String {
    route.int("number")?.let { number ->
        when (route.pattern) {
            Routes.PAGE, Routes.SURAH_PAGE, Routes.JUZ_PAGE -> return "$number-sahifa"
            Routes.SURAH, Routes.SURAH_PAGES -> {
                val surah = surahs.firstOrNull { it.number == number }
                return if (surah != null) "${surah.number}. ${surah.nameUz}" else "$number-sura"
            }
            Routes.JUZ, Routes.JUZ_PAGES -> return "$number-juz"
        }
    }
    return "Qur'on boʻylab oʻtish"
}

/**
 * «Qur'on bo'ylab o'tish» — sura, oyat, juz yoki sahifaga tez o'tish oynasi
 * (veb nusxadagi QuranNav). Tor ekranda sahifa boshida, keng ekranda navbarda turadi.
 */
@Composable
fun QuranNav(modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val shell = LocalShell.current
    val nav = LocalNav.current
    var open by remember { mutableStateOf(false) }

    Box(modifier) {
        Row(
            Modifier
                .clip(HolyTheme.shape)
                .clickable { open = !open }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                currentLabel(shell.route, shell.surahs),
                color = colors.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                HolyIcons.ChevronUp,
                null,
                tint = colors.ink,
                modifier = Modifier.size(14.dp).rotate(if (open) 0f else 180f),
            )
        }

        if (open) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            Popup(
                offset = IntOffset(0, with(density) { 40.dp.roundToPx() }),
                onDismissRequest = { open = false },
                properties = PopupProperties(focusable = true),
            ) {
                QuranNavPanel(
                    surahs = shell.surahs,
                    onClose = { open = false },
                    onGo = { route ->
                        open = false
                        nav.open(route)
                    },
                )
            }
        }
    }
}

@Composable
private fun QuranNavPanel(
    surahs: List<Surah>,
    onClose: () -> Unit,
    onGo: (String) -> Unit,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val listHeight = (screenHeight * 0.46f).dp

    var tab by rememberSaveable { mutableStateOf("surah") }
    var query by rememberSaveable { mutableStateOf("") }
    var picked by remember { mutableStateOf<Surah?>(null) }

    val q = query.trim().lowercase()
    val filteredSurahs = if (q.isEmpty() || (tab != "surah" && tab != "verse")) surahs else surahs.filter {
        it.number.toString().contains(q) || it.nameUz.lowercase().contains(q) ||
            (it.meaningUz ?: "").lowercase().contains(q)
    }
    val numbers = (if (tab == "juz") 1..30 else 1..604).filter { q.isEmpty() || it.toString().contains(query.trim()) }

    Column(
        Modifier
            .width(minOf(screenWidth * 0.92f, 460f).dp)
            .shadow(24.dp, shape)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.line, shape)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(colors.surface2)
                    .border(1.dp, colors.line, shape)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                TABS.forEach { (value, label) ->
                    val active = tab == value
                    Text(
                        label,
                        color = if (active) colors.bg else colors.inkSoft,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(if (active) colors.ink else Color.Transparent)
                            .clickable {
                                tab = value
                                query = ""
                                if (value != "verse") picked = null
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Box(
                Modifier.size(28.dp).clip(shape).clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text("✕", color = colors.inkFaint, fontSize = 14.sp)
            }
        }

        HolyInput(
            value = query,
            onValueChange = { query = it },
            placeholder = when (tab) {
                "juz" -> "Juz qidirish…"
                "page" -> "Sahifa qidirish…"
                else -> "Sura qidirish…"
            },
            keyboardOptions = if (tab == "juz" || tab == "page") {
                KeyboardOptions(keyboardType = KeyboardType.Number)
            } else {
                KeyboardOptions.Default
            },
            minHeight = 42.dp,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        if (tab == "verse") {
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.weight(1.6f).heightIn(max = listHeight)) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        val active = picked?.number == surah.number
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(if (active) colors.surface2 else Color.Transparent)
                                .clickable { picked = surah }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("${surah.number}", color = colors.inkFaint, fontSize = 13.sp, modifier = Modifier.width(22.dp))
                            Text(
                                surah.nameUz,
                                color = if (active) colors.ink else colors.inkSoft,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Box(Modifier.weight(1f).heightIn(max = listHeight)) {
                    val surah = picked
                    if (surah == null) {
                        Text("Avval surani tanlang", color = colors.inkFaint, fontSize = 12.sp, modifier = Modifier.padding(12.dp, 8.dp))
                    } else if (surah.totalAyahs <= 0) {
                        Text(
                            "Bu surada oyatlar soni koʻrsatilmagan",
                            color = colors.inkFaint,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp, 8.dp),
                        )
                    } else {
                        LazyColumn {
                            items((1..surah.totalAyahs).toList()) { verse ->
                                NavRow("$verse") { onGo(Routes.surah(surah.number, verse)) }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.padding(top = 12.dp).heightIn(max = listHeight)) {
                if (tab == "surah") {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .clickable { onGo(Routes.surah(surah.number)) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("${surah.number}", color = colors.inkFaint, fontSize = 13.sp, modifier = Modifier.width(22.dp))
                            Text(
                                surah.nameUz,
                                color = colors.inkSoft,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(surah.nameArabic, color = colors.inkFaint, fontFamily = AmiriQuran, fontSize = 14.sp)
                        }
                    }
                    if (filteredSurahs.isEmpty()) {
                        item { Text("Sura topilmadi", color = colors.inkFaint, fontSize = 12.sp, modifier = Modifier.padding(12.dp)) }
                    }
                } else {
                    items(numbers) { number ->
                        NavRow(if (tab == "juz") "$number-juz" else "$number-sahifa") {
                            onGo(if (tab == "juz") Routes.juz(number) else Routes.page(number))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        color = HolyTheme.colors.inkSoft,
        fontSize = 14.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(HolyTheme.shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
