package com.example.kotlin_holy.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.domain.model.reminderTypeLabel
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.AyahExtras
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.Diamond
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.HolyInput
import com.example.kotlin_holy.ui.components.HolyTabs
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.ListBox
import com.example.kotlin_holy.ui.components.ListSkeleton
import com.example.kotlin_holy.ui.components.MushafPageView
import com.example.kotlin_holy.ui.components.ReadStatus
import com.example.kotlin_holy.ui.components.ReadToggle
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.components.SurahGlyph
import com.example.kotlin_holy.ui.components.TranslationFlow
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.mixOver
import com.example.kotlin_holy.ui.components.readTileBorder
import com.example.kotlin_holy.ui.components.rowBackground
import com.example.kotlin_holy.ui.feature.hadith.RandomHadithCard
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.PageWidth
import com.example.kotlin_holy.ui.theme.AmiriQuran
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ReadingText

private val HOME_TABS = listOf(
    "reminder" to "Bugungi eslatma",
    "kursi" to "Oyatal Kursiy",
    "omanar" to "Aamanar-Rasulu",
    "hadith" to "Tasodifiy hadis",
)

/** Suraning ma'nosi, bo'lmasa Makkiy/Madaniy */
private fun surahSubtitle(surah: Surah): String = surah.meaningUz ?: when (surah.revelationPlace) {
    "makka" -> "Makkiy"
    "madina" -> "Madaniy"
    else -> "—"
}

private fun surahStatus(xatm: Map<Int, Long>, number: Int): ReadStatus? =
    JuzCatalog.surahPageRange(number)?.let { range -> ReadStatus(range.count { xatm.containsKey(it) }, range.count()) }

/** Bosh sahifa: eslatma va doimiy oyatlar tablari, so'ng suralar ro'yxati */
@Composable
fun HomeScreen(
    lists: QuranListsViewModel = hiltViewModel(),
    home: HomeViewModel = hiltViewModel(),
) {
    val state by lists.state.collectAsStateWithLifecycle()
    val reminders by home.reminders.collectAsStateWithLifecycle()
    val excerpts by home.excerpts.collectAsStateWithLifecycle()
    val saved by home.saved.collectAsStateWithLifecycle()
    val tafsir by home.tafsir.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    var tab by rememberSaveable { mutableStateOf(HOME_TABS.first().first) }
    var query by rememberSaveable { mutableStateOf("") }
    val order = state.settings.surahOrder
    val view = state.settings.listView

    val visible = remember(state.surahs, query, order) {
        val q = query.trim().lowercase()
        val filtered = if (q.isEmpty()) state.surahs else state.surahs.filter {
            it.number.toString().contains(q) || it.nameUz.lowercase().contains(q) ||
                (it.meaningUz ?: "").lowercase().contains(q) || it.nameArabic.contains(query.trim())
        }
        if (order == "asc") filtered.sortedBy { it.number } else filtered.sortedByDescending { it.number }
    }

    HolyPage(maxWidth = PageWidth.Section) {
        item(key = "tabs") {
            HolyTabs(HOME_TABS, tab, { tab = it }, Modifier.padding(bottom = 12.dp))
        }
        item(key = "tab-content") {
            Box(Modifier.padding(bottom = 24.dp)) {
                when (tab) {
                    "reminder" -> TodayReminder(reminders, onAdd = { nav.open(Routes.settings("reminders")) })
                    "hadith" -> RandomHadithCard()
                    else -> {
                        val excerpt = EXCERPTS.first { it.id == tab }
                        LaunchedEffect(excerpt.id) { home.loadExcerpt(excerpt) }
                        MushafExcerpt(
                            caption = excerpt.caption,
                            verses = excerpt.ayahs.map { "${excerpt.surah}:$it" }.toSet(),
                            data = excerpts[excerpt.id],
                            extras = AyahExtras(tafsir, saved, home::toggleSave),
                        )
                    }
                }
            }
        }
        item(key = "toolbar") {
            FlowRow(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                HolyInput(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Sura qidirish…",
                    leadingIcon = HolyIcons.Search,
                    modifier = Modifier.fillMaxWidth().widthIn(max = 280.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OrderToggle(order) { lists.setSurahOrder(if (order == "asc") "desc" else "asc") }
                    ViewToggle(view, lists::setListView)
                }
            }
        }
        if (state.error != null) {
            item(key = "error") { AlertBox(state.error, Modifier.padding(bottom = 16.dp)) }
        }
        when {
            state.loading -> item(key = "loading") {
                if (view == "list") ListSkeleton() else SurahGridSkeleton()
            }

            visible.isEmpty() -> item(key = "empty") {
                EmptyState(
                    title = if (query.isNotBlank()) "Hech narsa topilmadi" else "Hozircha sura yoʻq",
                    hint = if (query.isNotBlank()) "Boshqa soʻz bilan qidirib koʻring." else "assets/data/surahs.json fayli topilmadi yoki boʻsh.",
                )
            }

            view == "list" -> item(key = "list") {
                ListBox {
                    visible.forEachIndexed { index, surah ->
                        SurahListRow(
                            surah = surah,
                            index = index,
                            status = surahStatus(state.xatm, surah.number),
                            onOpen = { nav.open(Routes.surahPages(surah.number)) },
                            onToggle = { lists.toggleSurah(surah.number) },
                        )
                    }
                }
            }

            else -> item(key = "grid") {
                BoxWithConstraints {
                    val columns = when {
                        maxWidth >= 1200.dp -> 3
                        maxWidth >= 640.dp -> 2
                        else -> 1
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        visible.chunked(columns).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { surah ->
                                    SurahCard(
                                        surah = surah,
                                        status = surahStatus(state.xatm, surah.number),
                                        onOpen = { nav.open(Routes.surahPages(surah.number)) },
                                        onToggle = { lists.toggleSurah(surah.number) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(columns - row.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** «TARTIB: O'sish ˄» — suralar va sahifalar ro'yxatidagi tartib almashtirgich */
@Composable
fun OrderToggle(order: String, onToggle: () -> Unit) {
    val colors = HolyTheme.colors
    Row(
        Modifier.clip(HolyTheme.shape).clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("TARTIB:", color = colors.inkFaint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
        Text(
            if (order == "asc") "OʻSISH" else "KAMAYISH",
            color = colors.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
        )
        Icon(
            HolyIcons.ChevronUp,
            null,
            tint = colors.inkFaint,
            modifier = Modifier.size(14.dp).rotate(if (order == "asc") 0f else 180f),
        )
    }
}

/* ---------------- Bugungi eslatma ---------------- */

private val ARABIC_LETTER = Regex("[؀-ۿݐ-ݿࢠ-ࣿﭐ-﷿ﹰ-﻿]")
private val LATIN_LETTER = Regex("[A-Za-z]")

/* Arabcha harflari ko'p satr arabcha shriftda, o'ngdan chapga chiqadi */
private fun isArabicLine(line: String) = ARABIC_LETTER.findAll(line).count() > LATIN_LETTER.findAll(line).count()

/** Eslatma matni: har bir satr alohida — arabcha oyat va tarjimasi aralash bo'lishi mumkin */
@Composable
fun ReminderText(text: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    val colors = HolyTheme.colors
    val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
        lines.forEach { line ->
            if (isArabicLine(line)) {
                Text(
                    line,
                    style = if (compact) ReadingText.arabic(AmiriQuran, baseSize = 24f) else ReadingText.arabic(AmiriQuran),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    line,
                    style = if (compact) {
                        ReadingText.uzSub.copy(color = colors.inkSoft, fontSize = 14.sp)
                    } else {
                        ReadingText.uz
                    },
                )
            }
        }
    }
}

@Composable
private fun TodayReminder(reminders: List<Reminder>?, onAdd: () -> Unit) {
    val colors = HolyTheme.colors
    val list = reminders ?: return
    val reminder = remember(list) { SessionReminder.pick(list) }

    if (reminder == null) {
        HolyCard(Modifier.fillMaxWidth()) {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Hali eslatma yoʻq — oʻzingiz tanlagan oyat, hadis yoki hikmatni qoʻshing, dastur har ochilganda ulardan biri shu yerda chiqadi.",
                    color = colors.inkFaint,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                HolyTextButton("Eslatma qoʻshish", onAdd, variant = ButtonVariant.Primary, size = ButtonSize.Sm)
            }
        }
        return
    }

    val tone = if (reminder.type == "ayah") BadgeTone.Accent else BadgeTone.Neutral
    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                HolyBadge(reminderTypeLabel(reminder.type), tone = tone)
                if (list.size > 1) Text("${list.size} ta eslatmadan biri", color = colors.inkFaint, fontSize = 12.sp)
                if (reminder.title.isNotBlank()) HolyBadge(reminder.title, tone = tone)
            }
            ReminderText(reminder.text, Modifier.padding(top = 8.dp))
        }
    }
}

/* ---------------- Doimiy oyatlar ---------------- */

/**
 * Mushaf betidan bir yoki bir necha oyat — kitobdagi satrlari va o'rni bilan, ostida
 * o'zbekcha tarjimasi. Bosh sahifadagi doimiy tablar (Oyatal Kursiy va h.k.) uchun.
 */
@Composable
private fun MushafExcerpt(caption: String, verses: Set<String>, data: ExcerptData?, extras: AyahExtras) {
    val colors = HolyTheme.colors
    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(caption, color = colors.inkFaint, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            val mushaf = data?.mushaf
            if (mushaf == null) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 16.dp)) {
                    repeat(4) { Skeleton(Modifier.fillMaxWidth().height(28.dp)) }
                }
            } else {
                MushafPageView(page = mushaf, onlyVerses = verses)
            }
            if (data != null && data.ayahs.isNotEmpty()) {
                Divider(Modifier.padding(top = 16.dp))
                TranslationFlow(data.ayahs, extras, Modifier.padding(top = 16.dp))
            }
        }
    }
}

/* ---------------- Suralar ---------------- */

/** Sura mushafning qaysi betlarida: oralig'i («2–49») ustida, betlar soni («48 bet») ostida */
@Composable
private fun SurahPagesLabel(number: Int) {
    val range = JuzCatalog.surahPageRange(number) ?: return
    val colors = HolyTheme.colors
    Column(horizontalAlignment = Alignment.End) {
        Text(
            if (range.first == range.last) "${range.first}" else "${range.first}–${range.last}",
            color = colors.inkFaint,
            fontSize = 11.sp,
            lineHeight = 14.sp,
        )
        Text("${range.count()} bet", color = colors.inkFaint, fontSize = 11.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun StatusLine(status: ReadStatus?) {
    if (status == null || status.read <= 0) return
    Text(
        if (status.done) "✓ O'qildi" else "${status.read}/${status.total} bet o'qildi",
        color = HolyTheme.colors.read,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun SurahCard(
    surah: Surah,
    status: ReadStatus?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val done = status?.done == true
    HolyCard(
        modifier = modifier,
        borderColor = if (done) readTileBorder() else null,
        background = if (done) mixOver(colors.surface, colors.read, 0.07f) else null,
        onClick = onOpen,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Diamond("${surah.number}")
                Column(Modifier.weight(1f)) {
                    Text(surah.nameUz, color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(surahSubtitle(surah), color = colors.inkFaint, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    StatusLine(status)
                }
                Row(
                    Modifier.padding(end = 16.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    SurahPagesLabel(surah.number)
                    Column(horizontalAlignment = Alignment.End) {
                        SurahGlyph(surah.number, fontSize = 28, color = colors.ink, lineHeight = 23)
                        Text("${surah.totalAyahs} oyat", color = colors.inkFaint, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (status != null) {
                ReadToggle(
                    done = done,
                    partial = status.read > 0,
                    onToggle = onToggle,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    contentDescription = if (done) "${surah.nameUz} surasi belgisini olib tashlash" else "${surah.nameUz} surasini o'qildi deb belgilash",
                )
            }
        }
    }
}

@Composable
private fun SurahListRow(
    surah: Surah,
    index: Int,
    status: ReadStatus?,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val done = status?.done == true
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (done) mixOver(rowBackground(index), colors.read, 0.07f) else rowBackground(index))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("${surah.number}", color = colors.inkFaint, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(40.dp))
        Column(Modifier.weight(1f)) {
            Text(surah.nameUz, color = colors.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(surahSubtitle(surah), color = colors.inkFaint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            StatusLine(status)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SurahPagesLabel(surah.number)
            Column(horizontalAlignment = Alignment.End) {
                SurahGlyph(surah.number, fontSize = 26, color = colors.ink, lineHeight = 21)
                Text("${surah.totalAyahs} oyat", color = colors.inkFaint, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            if (status != null) ReadToggle(done = done, partial = status.read > 0, onToggle = onToggle)
        }
    }
}

@Composable
private fun SurahGridSkeleton() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(9) { Skeleton(Modifier.fillMaxWidth().height(86.dp)) }
    }
}
