package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.repository.ReadingTimeRepository
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.shell.ReadingTracker
import com.example.kotlin_holy.ui.theme.HolyColors
import com.example.kotlin_holy.ui.theme.HolyTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val MINUTE = 60_000L
private val MONTHS = listOf(
    "yanvar", "fevral", "mart", "aprel", "may", "iyun",
    "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr",
)
private val MONTHS_SHORT = listOf("Yan", "Fev", "Mar", "Apr", "May", "Iyn", "Iyl", "Avg", "Sen", "Okt", "Noy", "Dek")

/* Hafta dushanbadan boshlanadi */
private val WEEKDAYS = listOf("dushanba", "seshanba", "chorshanba", "payshanba", "juma", "shanba", "yakshanba")
private val WEEKDAY_LABELS = listOf("Du", "", "Ch", "", "Ju", "", "")

/* Kalendar katagi: bitta yashil rangning pog'onalari — kam → ko'p (chegaralar qat'iy, daqiqada) */
private val LEVEL_MINUTES = listOf(0, 1, 15, 30, 60)
private val LEVEL_ALPHA = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)

private const val CELL = 12
private const val GAP = 2
private const val STEP = CELL + GAP
private const val LABEL_COLUMN = 28
private const val MONTH_ROW = 16
private const val LAST_YEAR = "last"

data class ReadingTimeState(
    /** Kunlar bo'yicha vaqt: saqlangan + hali yozilmagan */
    val totals: Map<String, Long> = emptyMap(),
    /** O'sha kuni nechta bet «o'qildi» deb belgilangan */
    val marksByDay: Map<String, Int> = emptyMap(),
) {
    val total: Long get() = totals.values.filter { it > 0 }.sum()
    val readDays: Int get() = totals.values.count { it >= MINUTE }
}

@HiltViewModel
class ReadingTimeSettingsViewModel @Inject constructor(
    private val repository: ReadingTimeRepository,
    private val tracker: ReadingTracker,
    xatmRepository: XatmRepository,
) : ViewModel() {

    val state: StateFlow<ReadingTimeState> = combine(
        repository.totals,
        tracker.unsaved,
        xatmRepository.readPages,
    ) { saved, unsaved, pages ->
        val totals = HashMap(saved)
        unsaved.forEach { (day, ms) -> totals[day] = (totals[day] ?: 0L) + ms }
        ReadingTimeState(
            totals = totals.filterValues { it > 0 },
            marksByDay = pages.values.groupingBy { ReadingTracker.dayKey(it) }.eachCount(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingTimeState())

    /** Joriy seans davom etadi, lekin tozalashdan oldingi oraliq hisobga kirmaydi */
    fun reset() {
        tracker.resetSession()
        viewModelScope.launch { repository.reset() }
    }
}

/** Sozlamalar → O'qish vaqti (veb nusxadagi ReadingTimeSettings.jsx) */
internal fun LazyListScope.readingTimeSettings() {
    item(key = "reading-calendar") { Box(Modifier.padding(bottom = 20.dp)) { ReadingCalendarCard() } }
    item(key = "reading-reset") { Box(Modifier.padding(bottom = 20.dp)) { ReadingReset() } }
    item(key = "reading-note") {
        HintText(
            "Vaqt faqat sura, sahifa va juz oʻqish sahifalarida hisoblanadi. 1 daqiqa davomida " +
                "ekranga teginish yoki varaqlash boʻlmasa, oʻsha oraliq hisobga kirmaydi; ilova " +
                "yopilganda yoki fonga oʻtganda hisob toʻxtaydi.",
            modifier = Modifier.padding(bottom = 20.dp),
            relaxed = true,
        )
    }
}

/* ---------------- Sana yordamchilari ---------------- */

private fun weekdayIndex(date: LocalDate): Int = date.dayOfWeek.value - 1

/** 5-oktabr 2026, yakshanba */
private fun longDate(date: LocalDate): String =
    "${date.dayOfMonth}-${MONTHS[date.monthValue - 1]} ${date.year}, ${WEEKDAYS[weekdayIndex(date)]}"

private fun levelOf(ms: Long): Int {
    val minutes = ms / MINUTE
    return LEVEL_MINUTES.indices.lastOrNull { minutes >= LEVEL_MINUTES[it] } ?: 0
}

/* 1 daqiqaga yetmagan vaqt ham ko'rinsin */
private fun durationLabel(ms: Long): String =
    if (ms in 1 until MINUTE) "1 daqiqadan kam" else ReadingTracker.formatDuration(ms)

private fun levelColor(colors: HolyColors, level: Int): Color =
    if (level == 0) colors.line else colors.read.copy(alpha = LEVEL_ALPHA[level])

private data class DayCell(
    val date: LocalDate,
    val key: String,
    val ms: Long,
    val visible: Boolean,
    val future: Boolean,
    val level: Int,
)

/**
 * «So'nggi yil» — joriy haftagacha 53 hafta; yil tanlansa — 1-yanvardan 31-dekabrgacha
 * (hali kelmagan kunlar xira katak bo'lib ko'rinadi). Ustun — hafta, qator — hafta kuni.
 */
private fun buildWeeks(range: String, today: LocalDate, totals: Map<String, Long>): List<List<DayCell>> {
    val year = range.toIntOrNull()
    val first = if (year == null) today.minusDays((weekdayIndex(today) + 52 * 7).toLong()) else LocalDate.of(year, 1, 1)
    val last = if (year == null) today else LocalDate.of(year, 12, 31)
    val columns = mutableListOf<MutableList<DayCell>>()
    var day = first.minusDays(weekdayIndex(first).toLong())
    while (!day.isAfter(last) || weekdayIndex(day) != 0) {
        if (weekdayIndex(day) == 0) columns.add(mutableListOf())
        val key = day.toString()
        val ms = totals[key] ?: 0L
        columns.last().add(
            DayCell(
                date = day,
                key = key,
                ms = ms,
                visible = !day.isBefore(first) && !day.isAfter(last),
                future = day.isAfter(today),
                level = levelOf(ms),
            ),
        )
        day = day.plusDays(1)
    }
    return columns
}

/** Oy nomlari: oyning 1-kuni tushgan ustun; boshidagi chala oy ham (kamida 3 hafta bo'lsa) */
private fun monthLabels(weeks: List<List<DayCell>>): List<Pair<Int, Int>> {
    val labels = mutableListOf<Pair<Int, Int>>()
    weeks.forEachIndexed { column, week ->
        week.firstOrNull { it.visible && it.date.dayOfMonth == 1 }?.let { labels.add(column to it.date.monthValue - 1) }
    }
    val firstVisible = weeks.indexOfFirst { week -> week.any { it.visible } }
    if (firstVisible >= 0 && (labels.isEmpty() || labels[0].first - firstVisible >= 3)) {
        val cell = weeks[firstVisible].first { it.visible }
        labels.add(0, firstVisible to cell.date.monthValue - 1)
    }
    return labels
}

/* ---------------- Kalendar ---------------- */

@Composable
private fun ReadingCalendarCard() {
    val viewModel: ReadingTimeSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    ReadingCalendar(state.totals, state.marksByDay, today)
}

/** GitHub uslubidagi kalendar: ustun — hafta, qator — hafta kuni, rang — o'sha kungi vaqt */
@Composable
private fun ReadingCalendar(totals: Map<String, Long>, marksByDay: Map<String, Int>, today: LocalDate) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val density = LocalDensity.current

    val years = remember(totals, today) {
        (totals.keys.mapNotNull { it.take(4).toIntOrNull() } + today.year).distinct().sortedDescending()
    }
    var range by rememberSaveable { mutableStateOf(today.year.toString()) }
    /* Bosilgan katak — veb nusxadagi hover tooltip o'rniga */
    var selectedKey by remember(range) { mutableStateOf<String?>(null) }
    val weeks = remember(range, today, totals) { buildWeeks(range, today, totals) }
    val labels = remember(weeks) { monthLabels(weeks) }
    val rangeTotal = weeks.sumOf { week -> week.sumOf { if (it.visible) it.ms else 0L } }
    val rangeDays = weeks.sumOf { week -> week.count { it.visible && it.ms >= MINUTE } }

    val scroll = rememberScrollState()
    var wrapWidth by remember { mutableIntStateOf(0) }

    /* Tor ekranda: «So'nggi yil»da oxirgi haftalar, yil ko'rinishida bugungi hafta ko'rinib tursin */
    LaunchedEffect(weeks, range) {
        withFrameNanos { }
        if (range == LAST_YEAR) {
            scroll.scrollTo(scroll.maxValue)
        } else {
            val todayKey = today.toString()
            val column = weeks.indexOfFirst { week -> week.any { it.key == todayKey } }
            val target = if (column >= 0) {
                with(density) { (LABEL_COLUMN + GAP + (column + 1) * STEP).dp.roundToPx() } - wrapWidth
            } else {
                0
            }
            scroll.scrollTo(target.coerceIn(0, scroll.maxValue))
        }
    }

    SettingsCard {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            SectionTitle("Kalendar")
            Box(Modifier.horizontalScroll(rememberScrollState())) {
                Row(
                    Modifier
                        .clip(shape)
                        .background(colors.surface2)
                        .border(1.dp, colors.line, shape)
                        .padding(2.dp),
                ) {
                    (listOf(LAST_YEAR) + years.map(Int::toString)).forEach { item ->
                        val active = range == item
                        Text(
                            if (item == LAST_YEAR) "Soʻnggi yil" else item,
                            color = if (active) colors.bg else colors.inkSoft,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            style = tabular(),
                            modifier = Modifier
                                .clip(shape)
                                .background(if (active) colors.ink else Color.Transparent)
                                .clickable { range = item }
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .onSizeChanged { wrapWidth = it.width },
        ) {
            Box(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(bottom = 4.dp)) {
                Column {
                    Box(
                        Modifier
                            .padding(start = (LABEL_COLUMN + GAP).dp)
                            .width((weeks.size * STEP).dp)
                            .height(MONTH_ROW.dp),
                    ) {
                        labels.forEach { (column, month) ->
                            Text(
                                MONTHS_SHORT[month],
                                color = colors.inkFaint,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                modifier = Modifier.offset(x = (column * STEP).dp),
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(GAP.dp)) {
                        Column(Modifier.width(LABEL_COLUMN.dp), verticalArrangement = Arrangement.spacedBy(GAP.dp)) {
                            WEEKDAY_LABELS.forEach { label ->
                                Box(Modifier.height(CELL.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(label, color = colors.inkFaint, fontSize = 10.sp, lineHeight = 12.sp)
                                }
                            }
                        }
                        Canvas(
                            Modifier
                                .size(width = (weeks.size * STEP - GAP).dp, height = (7 * STEP - GAP).dp)
                                .pointerInput(weeks) {
                                    detectTapGestures { position ->
                                        val step = STEP.dp.toPx()
                                        val cell = weeks.getOrNull((position.x / step).toInt())
                                            ?.getOrNull((position.y / step).toInt())
                                        selectedKey = if (cell == null || !cell.visible || cell.future || cell.key == selectedKey) {
                                            null
                                        } else {
                                            cell.key
                                        }
                                    }
                                },
                        ) {
                            val step = STEP.dp.toPx()
                            val cell = CELL.dp.toPx()
                            val corner = CornerRadius(2.dp.toPx())
                            weeks.forEachIndexed { column, week ->
                                week.forEachIndexed { row, day ->
                                    if (day.visible) {
                                        val topLeft = Offset(column * step, row * step)
                                        drawRoundRect(
                                            color = if (day.future) colors.line.copy(alpha = 0.4f) else levelColor(colors, day.level),
                                            topLeft = topLeft,
                                            size = Size(cell, cell),
                                            cornerRadius = corner,
                                        )
                                        if (day.key == selectedKey) {
                                            val ring = 1.dp.toPx()
                                            drawRoundRect(
                                                color = colors.ink.copy(alpha = 0.7f),
                                                topLeft = topLeft - Offset(ring / 2, ring / 2),
                                                size = Size(cell + ring, cell + ring),
                                                cornerRadius = CornerRadius(corner.x + ring / 2),
                                                style = Stroke(ring),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            /* Tanlangan katak tooltip'i: avval qiymat, keyin sana */
            val selected = selectedKey?.let { key ->
                weeks.withIndex().firstNotNullOfOrNull { (column, week) ->
                    week.indexOfFirst { it.key == key }.takeIf { it >= 0 }?.let { row -> Triple(column, row, week[row]) }
                }
            }
            if (selected != null) {
                val (column, row, cell) = selected
                val x = with(density) { (LABEL_COLUMN + GAP + column * STEP + CELL / 2).dp.roundToPx() } - scroll.value
                val y = with(density) { (MONTH_ROW + row * STEP).dp.roundToPx() }
                val edge = with(density) { 90.dp.roundToPx() }
                CalendarTooltip(
                    x = x,
                    y = y,
                    align = when {
                        x < edge -> -1
                        x > wrapWidth - edge -> 1
                        else -> 0
                    },
                ) {
                    Text(
                        if (cell.ms > 0) durationLabel(cell.ms) else "Oʻqilmagan",
                        color = colors.bg,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(longDate(cell.date), color = colors.bg.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 16.sp)
                    marksByDay[cell.key]?.takeIf { it > 0 }?.let { marks ->
                        Text("$marks bet belgilangan", color = colors.bg.copy(alpha = 0.75f), fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }

        FlowRow(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            val strong = SpanStyle(color = colors.ink, fontWeight = FontWeight.SemiBold)
            Text(
                buildAnnotatedString {
                    append(if (range == LAST_YEAR) "Soʻnggi yilda" else "$range-yilda")
                    append(": ")
                    withStyle(strong) { append(ReadingTracker.formatDuration(rangeTotal)) }
                    append(" · ")
                    withStyle(strong) { append("$rangeDays kun") }
                    append(" oʻqilgan")
                },
                color = colors.inkFaint,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Kam", color = colors.inkFaint, fontSize = 11.sp, modifier = Modifier.padding(end = 4.dp))
                LEVEL_MINUTES.indices.forEach { level ->
                    Box(
                        Modifier
                            .size(CELL.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(levelColor(colors, level)),
                    )
                }
                Text("Koʻp", color = colors.inkFaint, fontSize = 11.sp, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

/** Katak ustidagi tooltip: bg-ink text-bg; [align] −1 — chapdan, 0 — markazdan, 1 — o'ngdan */
@Composable
private fun CalendarTooltip(x: Int, y: Int, align: Int, content: @Composable () -> Unit) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Column(
        Modifier
            .layout { measurable, _ ->
                val placeable = measurable.measure(Constraints())
                val shift = when (align) {
                    -1 -> 0
                    1 -> placeable.width
                    else -> placeable.width / 2
                }
                layout(0, 0) {
                    placeable.place(x - shift, y - placeable.height - 6.dp.roundToPx())
                }
            }
            .shadow(8.dp, shape)
            .clip(shape)
            .background(colors.ink)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        content()
    }
}

/* ---------------- Tozalash ---------------- */

@Composable
private fun ReadingReset() {
    val viewModel: ReadingTimeSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }

    SettingsCard {
        SectionTitle("Oʻqish vaqtini tozalash")
        HintText(
            "Barcha kunlar boʻyicha yozilgan oʻqish vaqti oʻchiriladi va hisob noldan boshlanadi. " +
                "Xatm belgilari va boshqa maʼlumotlarga taʼsir qilmaydi.",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 14.sp,
            relaxed = true,
        )
        ConfirmActions(
            confirming = confirming,
            question = "${ReadingTracker.formatDuration(state.total)} (${state.readDays} kun) oʻchirilsinmi?",
            triggerLabel = "Barchasini tozalash",
            confirmLabel = "Ha, tozalash",
            onTrigger = { confirming = true },
            onConfirm = {
                viewModel.reset()
                confirming = false
            },
            onCancel = { confirming = false },
            modifier = Modifier.padding(top = 20.dp),
            triggerEnabled = state.total > 0,
        )
    }
}
