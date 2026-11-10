package com.example.kotlin_holy.ui.feature.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.model.hadithReminderSource
import com.example.kotlin_holy.domain.model.numberLabel
import com.example.kotlin_holy.domain.repository.LibraryRepository
import com.example.kotlin_holy.domain.repository.RemindersRepository
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.Diamond
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.HolyProgress
import com.example.kotlin_holy.ui.components.ReadStatus
import com.example.kotlin_holy.ui.components.ReadStatusBadge
import com.example.kotlin_holy.ui.components.ReadToggle
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.readStatusLabel
import com.example.kotlin_holy.ui.components.readTileBackground
import com.example.kotlin_holy.ui.components.readTileBorder
import com.example.kotlin_holy.ui.components.rowBackground
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ReadingText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/* ---------------- Umumiy matn uslublari ---------------- */

/** tabular-nums — raqamlar bir xil kenglikda */
@Composable
internal fun tabular(): TextStyle = LocalTextStyle.current.copy(fontFeatureSettings = "tnum")

/** Bo'lim nomi: o'zbekcha bo'lmasa inglizcha, u ham bo'lmasa raqam */
internal fun sectionTitle(section: HadithSection): String =
    section.nameUz?.takeIf { it.isNotEmpty() }
        ?: section.nameEn?.takeIf { it.isNotEmpty() }
        ?: "${section.number}-boʻlim"

/** Hadis arabchasi (.arabic-hadith): KFGQPC Hafs, o'ngdan chapga */
@Composable
internal fun ArabicHadithText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Float = 18f,
    lineHeight: Float = 1.8f,
    maxLines: Int = Int.MAX_VALUE,
    color: androidx.compose.ui.graphics.Color = HolyTheme.colors.ink,
    preview: Boolean = true,
) {
    val hafs = rememberHafs()
    val annotated = remember(text, preview) { hafsText(text, if (preview) PREVIEW_CHARS else Int.MAX_VALUE) }
    Text(
        annotated,
        modifier = modifier,
        style = TextStyle(
            fontFamily = hafs,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * lineHeight).sp,
            textAlign = TextAlign.Right,
            textDirection = TextDirection.Rtl,
            color = color,
        ),
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
    )
}

/* ---------------- Belgilar ---------------- */

/** «12/148 hadis oʻqildi» — suralardagi «12/48 bet o'qildi» kabi, faqat nimadir belgilangan bo'lsa */
@Composable
internal fun ReadStatusLine(status: ReadStatus, modifier: Modifier = Modifier) {
    if (status.read <= 0) return
    Text(
        readStatusLabel(status),
        modifier = modifier,
        color = HolyTheme.colors.read,
        fontSize = 11.sp,
        lineHeight = 16.5.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = tabular(),
    )
}

/** Kartada / ro'yxat qatorida: hadis eslatmalarda saqlanganini bildiruvchi belgi (bosilmaydi) */
@Composable
internal fun SavedReminderMark(modifier: Modifier = Modifier) {
    Icon(
        HolyIcons.BookmarkFilled,
        contentDescription = "Eslatmalarda saqlangan",
        tint = HolyTheme.colors.accent,
        modifier = modifier.size(16.dp),
    )
}

/** Bitta hadisni o'qildi deb belgilash doirachasi (suralardagi SurahReadToggle kabi) */
@Composable
internal fun HadithReadToggle(number: Double, readAt: Long?, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val done = readAt != null
    val label = numberLabel(number)
    ReadToggle(
        done = done,
        onToggle = onToggle,
        modifier = modifier,
        contentDescription = if (done) "$label-hadis belgisini olib tashlash" else "$label-hadisni oʻqildi deb belgilash",
    )
}

/**
 * Butun bo'limni o'qildi deb belgilash yoki (hammasi belgilangan bo'lsa) belgilarni olib tashlash —
 * suralar ro'yxatidagi kabi. partial — bir qismi belgilangan bo'lim.
 */
@Composable
internal fun SectionReadToggle(section: HadithSection, status: ReadStatus, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    ReadToggle(
        done = status.done,
        onToggle = onToggle,
        modifier = modifier,
        partial = status.read > 0,
        contentDescription = if (status.done) {
            "${section.number}-boʻlim belgilarini olib tashlash"
        } else {
            "${section.number}-boʻlimni oʻqildi deb belgilash"
        },
    )
}

/* ---------------- Eslatmalarga saqlash ---------------- */

/** Hadisni «Eslatmalar»ga saqlash / olib tashlash */
@HiltViewModel
class HadithReminderViewModel @Inject constructor(
    private val remindersRepository: RemindersRepository,
    private val library: LibraryRepository,
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>> =
        remindersRepository.reminders.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /* Ketma-ket bosilganda bir hadis ikki marta saqlanib qolmasligi uchun */
    private val mutex = Mutex()

    fun toggle(collectionId: String, collectionName: String?, hadith: Hadith) {
        viewModelScope.launch {
            mutex.withLock {
                val source = hadithReminderSource(collectionId, hadith.number)
                val saved = remindersRepository.reminders.first().firstOrNull { it.source == source }
                if (saved != null) {
                    remindersRepository.delete(saved.id)
                    return@withLock
                }
                val name = collectionName
                    ?: runCatching { library.collection(collectionId)?.nameUz }.getOrNull()
                    ?: collectionId
                remindersRepository.add(
                    type = "hadith",
                    title = "$name, ${hadith.rangeLabel}-hadis",
                    text = reminderText(hadith),
                    source = source,
                )
            }
        }
    }

    /** Matn: rivoyatchi va o'zbekcha tarjima; tarjimasi yo'q hadisda — arabcha matn */
    private fun reminderText(hadith: Hadith): String {
        if (hadith.textUz.isNullOrEmpty()) return hadith.textArabic.orEmpty()
        return listOfNotNull(hadith.narratorUz, hadith.textUz).filter { it.isNotEmpty() }.joinToString("\n")
    }
}

/**
 * Hadisni «Eslatmalar»ga saqlash — oyatdagi AyahReminderButton kabi. Saqlangan hadisda belgi
 * to'ldirilgan; qayta bosilsa eslatmalardan olib tashlanadi. [collectionName] berilmasa —
 * kitob nomi to'plamlar ro'yxatidan olinadi (eslatma sarlavhasi: «Sahihi Buxoriy, 12-hadis»).
 */
@Composable
fun HadithReminderButton(
    collectionId: String,
    hadith: Hadith,
    modifier: Modifier = Modifier,
    collectionName: String? = null,
) {
    val viewModel: HadithReminderViewModel = hiltViewModel()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val source = hadithReminderSource(collectionId, hadith.number)
    val saved = reminders.any { it.source == source }
    val tint = if (saved) colors.accent else colors.inkSoft

    Row(
        modifier
            .clip(HolyTheme.shape)
            .clickable(onClickLabel = if (saved) "Eslatmalardan olib tashlash" else null) {
                viewModel.toggle(collectionId, collectionName, hadith)
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (saved) HolyIcons.BookmarkFilled else HolyIcons.Bookmark, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            if (saved) "Eslatmalarda saqlangan" else "Eslatmalarga saqlash",
            color = tint,
            fontSize = 13.sp,
            lineHeight = 19.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ---------------- Sahifa qismlari ---------------- */

/** Katak / ro'yxat almashtirgichi — ro'yxat ustida, o'ngda */
@Composable
internal fun ViewBar(view: String, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.End) {
        ViewToggle(view = view, onChange = onChange)
    }
}

/** Yuklanayotgan to'r: 9 ta kartochka o'rni */
@Composable
internal fun GridSkeleton(columns: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        (0 until 9).chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { _ -> Skeleton(Modifier.weight(1f).height(104.dp)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Xato va ostida «← Hadislar» havolasi */
@Composable
internal fun ErrorWithLink(message: String, link: String, onLink: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AlertBox(message)
        Text(
            link,
            color = HolyTheme.colors.accent,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(HolyTheme.shape).clickable(onClick = onLink),
        )
    }
}

/**
 * Veb nusxadagi «flex flex-wrap justify-end»: bir qatorga sig'sa o'ngda turadi, sig'masa
 * qatorlarga bo'linib chapdan tekislanadi (ichki blok butun kenglikni egallaydi).
 */
@Composable
internal fun WrapEndRow(gapX: Dp, gapY: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier.fillMaxWidth()) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val gx = gapX.roundToPx()
        val gy = gapY.roundToPx()
        val lines = mutableListOf<MutableList<Placeable>>()
        var lineWidth = 0
        placeables.forEach { placeable ->
            if (lines.isEmpty() || lineWidth + gx + placeable.width > width) {
                lines += mutableListOf(placeable)
                lineWidth = placeable.width
            } else {
                lines.last() += placeable
                lineWidth += gx + placeable.width
            }
        }
        val heights = lines.map { line -> line.maxOf { it.height } }
        val height = heights.sum() + gy * (lines.size - 1).coerceAtLeast(0)
        layout(width, height) {
            var y = 0
            lines.forEachIndexed { index, line ->
                val total = line.sumOf { it.width } + gx * (line.size - 1)
                var x = if (lines.size == 1) width - total else 0
                line.forEach { placeable ->
                    placeable.place(x, y + (heights[index] - placeable.height) / 2)
                    x += placeable.width + gx
                }
                y += heights[index] + gy
            }
        }
    }
}

/* ---------------- Kitob haqida ---------------- */

/**
 * Hadis kitobi haqida ma'lumot: nomi, muallifi, qisqa tavsifi va belgilar — Kitoblar ro'yxatidagi
 * kartochka bilan bir xil ma'lumot, kitob ochilganda bo'limlar ustida to'liq ko'rinadi.
 */
@Composable
internal fun HadithBookInfo(collection: HadithCollection, status: ReadStatus, sm: Boolean) {
    val colors = HolyTheme.colors
    val total = collection.totalHadiths
    val translated = collection.translatedHadiths

    HolyCard(Modifier.fillMaxWidth().padding(bottom = 28.dp).then(riseIn())) {
        Column(Modifier.padding(if (sm) 24.dp else 20.dp)) {
            val title: @Composable (Modifier) -> Unit = { titleModifier ->
                Column(titleModifier) {
                    Text(collection.nameUz, color = colors.ink, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp)
                    Text(
                        collection.authorUz,
                        color = colors.inkFaint,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            val arabic: @Composable (Modifier) -> Unit = { arabicModifier ->
                ArabicHadithText(
                    collection.nameArabic,
                    modifier = arabicModifier,
                    fontSize = 30f,
                    lineHeight = 1.375f,
                    color = colors.accent,
                    preview = false,
                )
            }
            if (sm) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    title(Modifier.weight(1f))
                    arabic(Modifier)
                }
            } else {
                /* Tor ekranda arabcha nomi tepada, o'ngda */
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    arabic(Modifier.fillMaxWidth())
                    title(Modifier)
                }
            }

            if (!collection.descriptionUz.isNullOrEmpty()) {
                Text(
                    collection.descriptionUz,
                    color = colors.inkSoft,
                    fontSize = 15.sp,
                    lineHeight = 24.4.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(top = 16.dp).widthIn(max = 896.dp),
                )
            }

            FlowRow(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HolyBadge("$total ta hadis")
                if (collection.sections > 1) HolyBadge("${collection.sections} ta boʻlim")
                if (translated < total) {
                    HolyBadge("$translated/$total tarjima qilingan", tone = BadgeTone.Accent)
                } else {
                    HolyBadge("Tarjima tayyor", tone = BadgeTone.Accent)
                }
                ReadStatusBadge(status)
            }
        }
    }
}

/* ---------------- /books: kitoblar ---------------- */

/** Kitoblar to'ridagi kartochka */
@Composable
internal fun BookCard(
    collection: HadithCollection,
    status: ReadStatus,
    modifier: Modifier,
    fill: Boolean,
    onClick: () -> Unit,
) {
    val colors = HolyTheme.colors
    HolyCard(
        modifier,
        borderColor = if (status.done) readTileBorder() else null,
        background = if (status.done) readTileBackground() else null,
        onClick = onClick,
    ) {
        Column(Modifier.padding(20.dp).then(if (fill) Modifier.fillMaxHeight() else Modifier)) {
            ArabicHadithText(
                collection.nameArabic,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 24f,
                lineHeight = 1.375f,
                color = colors.inkSoft,
                preview = false,
            )
            Text(
                collection.nameUz,
                color = colors.ink,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.45).sp,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                collection.authorUz,
                color = colors.inkFaint,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                collection.descriptionUz.orEmpty(),
                color = colors.inkSoft,
                fontSize = 14.sp,
                lineHeight = 22.75.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .then(if (fill) Modifier.weight(1f) else Modifier),
            )
            FlowRow(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HolyBadge("${collection.totalHadiths} ta hadis")
                if (collection.sections > 1) HolyBadge("${collection.sections} ta boʻlim")
                if (collection.translatedHadiths < collection.totalHadiths) {
                    HolyBadge(
                        "${collection.translatedHadiths}/${collection.totalHadiths} tarjima qilingan",
                        tone = BadgeTone.Accent,
                    )
                }
                /* Kitobdan nechta hadis o'qildi deb belgilangan */
                if (status.read > 0) HolyBadge(readStatusLabel(status), tone = BadgeTone.Read)
            }
        }
    }
}

/** Kitoblar ro'yxati qatori: raqam · nom · arabcha nomi */
@Composable
internal fun BookRow(index: Int, collection: HadithCollection, status: ReadStatus, onClick: () -> Unit) {
    val colors = HolyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (status.done) readTileBackground() else rowBackground(index))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${index + 1}",
            color = colors.inkFaint,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = tabular(),
            modifier = Modifier.width(32.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                collection.nameUz,
                color = colors.ink,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val translation = if (collection.translatedHadiths < collection.totalHadiths) {
                " · ${collection.translatedHadiths}/${collection.totalHadiths} tarjima"
            } else {
                ""
            }
            Text(
                "${collection.authorUz} · ${collection.totalHadiths} ta hadis$translation",
                color = colors.inkFaint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ReadStatusLine(status)
        }
        ArabicHadithText(
            collection.nameArabic,
            fontSize = 20f,
            lineHeight = 1.4f,
            color = colors.inkSoft,
            preview = false,
        )
    }
}

/* ---------------- /book/:id: bo'limlar ---------------- */

/** Bo'limlar to'ridagi kartochka */
@Composable
internal fun SectionTile(
    section: HadithSection,
    status: ReadStatus,
    modifier: Modifier,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val total = section.totalHadiths
    val translated = section.translatedHadiths
    val complete = translated == total

    HolyCard(
        modifier,
        borderColor = if (status.done) readTileBorder() else null,
        background = if (status.done) readTileBackground() else null,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Diamond(section.number.toString())
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        sectionTitle(section),
                        color = colors.ink,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val suffix = when {
                        complete -> " · tarjima tayyor"
                        translated == 0 -> " · tarjimasiz"
                        else -> ""
                    }
                    Text(
                        "$total ta hadis$suffix",
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                    ReadStatusLine(status, Modifier.padding(top = 2.dp))
                    if (!complete && translated > 0) {
                        Column(Modifier.padding(top = 8.dp)) {
                            HolyProgress(value = translated, total = total)
                            Text(
                                "$translated/$total tarjima qilingan",
                                color = colors.inkFaint,
                                fontSize = 11.sp,
                                lineHeight = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                style = tabular(),
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
            SectionReadToggle(section, status, onToggle, Modifier.align(Alignment.TopEnd).padding(6.dp))
        }
    }
}

/** Jadval uslubidagi bo'limlar ro'yxati qatori: raqam · nom · hadislar oralig'i · belgi */
@Composable
internal fun SectionRow(
    index: Int,
    count: Int,
    section: HadithSection,
    status: ReadStatus,
    sm: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val total = section.totalHadiths
    val translated = section.translatedHadiths
    Row(
        Modifier
            .fillMaxWidth()
            .listBoxRow(index, count, HolyTheme.radius, colors.line)
            .background(if (status.done) readTileBackground() else rowBackground(index))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            section.number.toString(),
            color = colors.inkFaint,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = tabular(),
            modifier = Modifier.width(40.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                sectionTitle(section),
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!section.nameEn.isNullOrEmpty() && !section.nameUz.isNullOrEmpty()) {
                Text(
                    section.nameEn,
                    color = colors.inkFaint,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            ReadStatusLine(status)
        }
        if (sm) {
            when {
                translated == total -> HolyBadge("tayyor", tone = BadgeTone.Accent)
                translated > 0 -> HolyBadge("$translated/$total")
            }
        }
        /* sections.json da oraliq bo'lmasa (firstHadith yo'q) — hadislar soni */
        if (section.firstHadith == 0.0 && section.lastHadith == 0.0) {
            Text("$total ta", color = colors.inkSoft, fontSize = 14.sp, lineHeight = 20.sp, style = tabular())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    numberLabel(section.firstHadith),
                    color = colors.inkSoft,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    style = tabular(),
                )
                Text(
                    "–",
                    color = colors.inkFaint,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Text(
                    numberLabel(section.lastHadith),
                    color = colors.inkSoft,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    style = tabular(),
                )
            }
        }
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            SectionReadToggle(section, status, onToggle)
        }
    }
}

/* ---------------- /book/:id/s/:section: hadislar ---------------- */

/** Hadislar to'ridagi kartochka */
@Composable
internal fun HadithTile(
    hadith: Hadith,
    readAt: Long?,
    saved: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val textUz = hadith.textUz
    val name = remember(hadith.narratorUz) { narratorName(hadith.narratorUz) }
    val excerpt = remember(textUz) { if (textUz.isNullOrEmpty()) "" else hadithExcerpt(textUz) }

    HolyCard(
        modifier,
        borderColor = if (readAt != null) readTileBorder() else null,
        background = if (readAt != null) readTileBackground() else null,
        onClick = onClick,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Diamond(hadith.label)
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    when {
                        textUz.isNullOrEmpty() -> {
                            /* Tarjimasi hali yo'q: sarlavha — arabcha matnning boshi */
                            ArabicHadithText(hadith.textArabic.orEmpty(), Modifier.fillMaxWidth(), maxLines = 2)
                            HolyBadge("Tarjimasiz", Modifier.padding(top = 8.dp))
                        }
                        name.isNotEmpty() -> {
                            Text(
                                name,
                                color = colors.ink,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                excerpt,
                                color = colors.inkFaint,
                                fontSize = 13.sp,
                                lineHeight = 17.9.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        else -> {
                            /* Rivoyatchi nomi yo'q (yoki uzun isnod zanjiri): sarlavha — mazmun, ostida arabcha matn */
                            Text(
                                excerpt,
                                color = colors.ink,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            ArabicHadithText(
                                hadith.textArabic.orEmpty(),
                                Modifier.fillMaxWidth().padding(top = 4.dp),
                                maxLines = 1,
                                color = colors.inkFaint,
                            )
                        }
                    }
                    if (!hadith.grade.isNullOrEmpty()) {
                        HolyBadge(gradeLabel(hadith.grade), Modifier.padding(top = 8.dp), tone = BadgeTone.Accent)
                    }
                }
            }
            HadithReadToggle(hadith.number, readAt, onToggle, Modifier.align(Alignment.TopEnd).padding(6.dp))
            /* O'qildi doirachasi ostida, o'sha ustunda */
            if (saved) SavedReminderMark(Modifier.align(Alignment.TopEnd).padding(top = 32.dp, end = 8.dp))
        }
    }
}

/** Jadval uslubidagi hadislar ro'yxati qatori: belgi · raqam · sarlavha · arabcha boshi */
@Composable
internal fun HadithRow(
    index: Int,
    count: Int,
    hadith: Hadith,
    readAt: Long?,
    saved: Boolean,
    md: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val colors = HolyTheme.colors
    val textUz = hadith.textUz
    val name = remember(hadith.narratorUz) { narratorName(hadith.narratorUz) }
    val excerpt = remember(textUz) { if (textUz.isNullOrEmpty()) "" else hadithExcerpt(textUz) }

    Row(
        Modifier
            .fillMaxWidth()
            .listBoxRow(index, count, HolyTheme.radius, colors.line)
            .background(if (readAt != null) readTileBackground() else rowBackground(index))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            HadithReadToggle(hadith.number, readAt, onToggle)
        }
        Text(
            hadith.label,
            color = colors.inkFaint,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = tabular(),
            modifier = Modifier.width(48.dp),
        )
        Column(Modifier.weight(1f)) {
            if (!textUz.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        name.ifEmpty { excerpt },
                        color = colors.ink,
                        fontSize = 15.sp,
                        lineHeight = 22.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (saved) SavedReminderMark()
                }
                if (name.isNotEmpty()) {
                    Text(
                        excerpt,
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tarjimasiz", color = colors.inkFaint, fontSize = 14.sp, lineHeight = 20.sp)
                    if (saved) SavedReminderMark()
                }
            }
        }
        if (md) {
            ArabicHadithText(
                hadith.textArabic.orEmpty(),
                Modifier.widthIn(max = 256.dp),
                maxLines = 1,
                color = colors.inkSoft,
            )
        }
    }
}

/* ---------------- /book/:id/s/:section/:number: bitta hadis ---------------- */

/**
 * Bitta hadis. mode — "both" | "arabic" | "translation".
 * "both" da keng ekranda arabcha (o'ngda) va o'zbekcha (chapda) yonma-yon, o'rtada ingichka chiziq;
 * tor ekranda arabcha tepada, o'zbekcha pastda, orasida gorizontal chiziq.
 */
@Composable
internal fun HadithCard(hadith: Hadith, mode: String, sm: Boolean, md: Boolean) {
    val colors = HolyTheme.colors
    val showArabic = mode != "translation"
    val showUz = mode != "arabic"
    val parallel = showArabic && showUz

    HolyCard(Modifier.fillMaxWidth().then(riseIn())) {
        Column(Modifier.padding(if (sm) 24.dp else 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Diamond(hadith.label)
                /* Manbada bir nechta raqamga bo'lingan rivoyat bitta matn bo'lib keladi (Buxoriy 299–301) */
                if (hadith.numberTo != null) {
                    Text(
                        hadith.rangeLabel,
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        style = tabular(),
                    )
                }
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (showUz && hadith.translation == "ai") HolyBadge("Sunʼiy intellekt tarjimasi")
                    if (!hadith.grade.isNullOrEmpty()) HolyBadge(gradeLabel(hadith.grade), tone = BadgeTone.Accent)
                }
            }

            SelectionContainer(Modifier.padding(top = 20.dp)) {
                val arabic: @Composable (Modifier) -> Unit = { arabicModifier ->
                    ArabicHadithText(
                        hadith.textArabic.orEmpty(),
                        arabicModifier,
                        fontSize = if (sm) 30f else 27f,
                        lineHeight = 2.3f,
                        preview = false,
                        color = colors.ink,
                    )
                }
                val uz: @Composable (Modifier) -> Unit = { uzModifier -> HadithUzText(hadith, uzModifier) }

                if (parallel && md) {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        uz(Modifier.weight(1f))
                        Spacer(Modifier.width(28.dp))
                        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                        Spacer(Modifier.width(28.dp))
                        arabic(Modifier.weight(1f))
                    }
                } else {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        if (showArabic) arabic(Modifier.fillMaxWidth())
                        if (parallel) Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        if (showUz) uz(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/** Hadisning o'zbekcha qismi: rivoyatchi, tarjima, manba */
@Composable
private fun HadithUzText(hadith: Hadith, modifier: Modifier) {
    Column(modifier) {
        /* Ba'zi to'plamlarda isnod va manba keltirilmagan — bo'sh qatorlar chiqmaydi */
        if (!hadith.narratorUz.isNullOrEmpty()) {
            Text(
                hadith.narratorUz,
                style = ReadingText.uzSub.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (!hadith.textUz.isNullOrEmpty()) {
            Text(hadith.textUz, style = ReadingText.uz)
        } else {
            Text("Tarjima hali kiritilmagan", style = ReadingText.uzSub.copy(fontStyle = FontStyle.Italic))
        }
        if (!hadith.sourceUz.isNullOrEmpty()) {
            Text(hadith.sourceUz, style = ReadingText.uzSub, modifier = Modifier.padding(top = 12.dp))
        }
    }
}
