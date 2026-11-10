package com.example.kotlin_holy.ui.components

import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.Surah
import com.example.kotlin_holy.ui.theme.HolyFonts
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.LocalTextScales
import com.example.kotlin_holy.ui.theme.ReadingText

/** public/data/tafsir dagi izohlar muallifi — quran.com dagi tarjimasi (#55) izohlaridan */
const val TAFSIR_AUTHOR = "Shayx Muhammad Sodiq Muhammad Yusuf"

/**
 * Oyatni eslatmalarga saqlash va tafsir — oyat tarjimasi bosilganda ochiladigan panel uchun
 * kerak bo'lgan hamma narsa. [tafsir] — "2:255" → matn, [saved] — saqlangan oyatlar ("2:255").
 */
data class AyahExtras(
    val tafsir: Map<String, String> = emptyMap(),
    val saved: Set<String> = emptySet(),
    val onToggleSave: (Ayah) -> Unit = {},
)

/* ---------------- Oyat paneli ---------------- */

/** Oyat tarjimasini «Eslatmalar»ga saqlash; saqlangan oyatda belgi to'ldirilgan */
@Composable
fun AyahReminderButton(saved: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val tint = if (saved) colors.accent else colors.inkSoft
    Row(
        modifier.clip(HolyTheme.shape).clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (saved) HolyIcons.BookmarkFilled else HolyIcons.Bookmark, null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            if (saved) "Eslatmalarda saqlangan" else "Eslatmalarga saqlash",
            color = tint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** Oyat ostida ochiladigan panel: eslatmaga saqlash va (bo'lsa) tafsir */
@Composable
fun AyahPanel(
    open: Boolean,
    saved: Boolean,
    onToggleSave: () -> Unit,
    tafsir: String?,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val radius = HolyTheme.radius
    Collapse(open) {
        Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(colors.accent))
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(topEnd = radius, bottomEnd = radius))
                    .background(colors.surface2)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                AyahReminderButton(saved, onToggleSave)
                if (tafsir != null) {
                    Divider(Modifier.padding(top = 10.dp))
                    Text(
                        "Tafsir · $TAFSIR_AUTHOR",
                        color = colors.inkFaint,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                    )
                    Text(tafsir, style = ReadingText.uzSub.copy(color = colors.inkSoft))
                }
            }
        }
    }
}

/* ---------------- Tarjima oqimi ---------------- */

/**
 * Sahifa tarjimasi: raqamlar matn ichida qalin holda. Dastur rangida: tafsiri bor oyatning
 * raqami, eslatmalarga saqlangan oyatning matni. Oyat bosilsa, panel shu oyatdan keyin ochiladi.
 */
@Composable
fun TranslationFlow(ayahs: List<Ayah>, extras: AyahExtras, modifier: Modifier = Modifier) {
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    val colors = HolyTheme.colors
    val style = ReadingText.uz
    val lineHeight = style.fontSize * 2

    /* Ochiq oyatdan keyin oqim uziladi: [oyatlar…][panel][qolgan oyatlar…] */
    val openIndex = ayahs.indexOfFirst { it.verseKey == openKey }
    val chunks = if (openIndex < 0) listOf(ayahs) else listOf(ayahs.take(openIndex + 1), ayahs.drop(openIndex + 1))

    Column(modifier.fillMaxWidth()) {
        chunks.forEachIndexed { index, chunk ->
            if (chunk.isNotEmpty()) {
                val text = buildAnnotatedString {
                    chunk.forEach { ayah ->
                        val key = ayah.verseKey
                        val hasTafsir = extras.tafsir.containsKey(key)
                        val saved = key in extras.saved
                        val body: AnnotatedString.Builder.() -> Unit = {
                            withStyle(SpanStyle(color = if (hasTafsir) colors.accent else colors.ink, fontWeight = FontWeight.Bold)) {
                                append("${ayah.numberInSurah}.")
                            }
                            append(" ")
                            val translation = ayah.translationUz
                            if (translation.isNullOrBlank()) {
                                withStyle(SpanStyle(color = colors.inkFaint, fontStyle = FontStyle.Italic)) {
                                    append("tarjima kiritilmagan")
                                }
                            } else {
                                withStyle(SpanStyle(color = if (saved) colors.accent else colors.inkSoft)) {
                                    append(translation)
                                }
                            }
                            append("  ")
                        }
                        if (!ayah.translationUz.isNullOrBlank()) {
                            withLink(
                                LinkAnnotation.Clickable(
                                    tag = key,
                                    styles = TextLinkStyles(
                                        style = if (openKey == key) SpanStyle(background = colors.surface2) else null,
                                    ),
                                ) { openKey = if (openKey == key) null else key },
                            ) { body() }
                        } else {
                            body()
                        }
                    }
                }
                Text(text, style = style.copy(lineHeight = lineHeight))
            }
            if (index == 0 && openIndex >= 0) {
                val ayah = ayahs[openIndex]
                AyahPanel(
                    open = true,
                    saved = ayah.verseKey in extras.saved,
                    onToggleSave = { extras.onToggleSave(ayah) },
                    tafsir = extras.tafsir[ayah.verseKey],
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                )
            }
        }
    }
}

/**
 * Mushaf beti har doim to'liq chiqadi (oldingi sura oxiri ham), shuning uchun tarjima ham
 * butun bet bo'yicha. Betda ikki sura bo'lsa, oyat raqamlari aralashmasligi uchun sura nomi
 * bilan ajratiladi.
 */
@Composable
fun PageTranslation(ayahs: List<Ayah>, extras: AyahExtras, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val segments = remember(ayahs) {
        val result = mutableListOf<Pair<Ayah, MutableList<Ayah>>>()
        ayahs.forEach { ayah ->
            val last = result.lastOrNull()
            if (last != null && last.first.surahNumber == ayah.surahNumber) last.second += ayah
            else result += ayah to mutableListOf(ayah)
        }
        result
    }
    if (segments.size <= 1) {
        TranslationFlow(ayahs, extras, modifier)
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        segments.forEach { (first, list) ->
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(Modifier.weight(1f).height(1.dp).background(colors.line))
                    Text(
                        "${first.surahNumber}. ${first.surahNameUz ?: "sura"}",
                        color = colors.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Box(Modifier.weight(1f).height(1.dp).background(colors.line))
                }
                TranslationFlow(list, extras)
            }
        }
    }
}

/* ---------------- Oyat bo'yicha rejim ---------------- */

/** Qiroat holati oyat qatori uchun (faqat Android nusxasida) */
data class AyahAudio(
    /** Shu sura qiroati yuklanganmi */
    val isReady: (Int) -> Boolean,
    val playingVerse: String?,
    val playingWord: Int?,
    val isPlaying: Boolean,
    val onPlay: (Ayah) -> Unit,
    val onPause: () -> Unit,
    val onStop: () -> Unit,
    val onWord: (Ayah, Int) -> Unit,
)

private const val BASE_FONT_PX = 100f

/**
 * Oyat bo'yicha rejimda arabcha matn sahifa rejimidagi KFGQPC shriftida chiqadi: so'zlar oyat
 * turgan mushaf betidan olinadi. Harf o'lchami sahifa rejimidagidek: betning eng uzun satri
 * ustun eniga sig'adigan o'lcham — ikkala rejimda harflar bir xil bo'ladi.
 * Bet topilmasa oddiy Unicode matn chiqadi.
 */
@Composable
fun MushafAyah(
    ayah: Ayah,
    page: MushafPage?,
    modifier: Modifier = Modifier,
    playingWord: Int? = null,
    playingVerse: Boolean = false,
    onWordClick: ((Int) -> Unit)? = null,
    lineHeightRatio: Float = 2.3f,
) {
    val colors = HolyTheme.colors
    val words = page?.lines?.flatMap { it.words }
        ?.filter { it.surahNumber == ayah.surahNumber && it.ayahNumber == ayah.numberInSurah }
        .orEmpty()

    if (page == null || words.isEmpty()) {
        val context = LocalContext.current
        val family = remember { HolyFonts.uthmani(context.assets) }
        Text(ayah.textArabic, style = ReadingText.arabic(family), modifier = modifier.fillMaxWidth())
        return
    }

    val (family, typeface) = rememberMushafFont(page.pageNumber)
    val density = LocalDensity.current
    val scale = LocalTextScales.current.arabic
    val layout = remember { mutableStateOf<TextLayoutResult?>(null) }

    /* So'zlarning matndagi o'rni — bosilgan so'zni topish uchun */
    val spans = remember(words) {
        var start = 0
        words.mapIndexed { index, word ->
            val from = start + if (index > 0) 1 else 0
            start = from + word.code.length
            from until start to word
        }
    }
    val text = remember(words, playingWord, playingVerse, colors) {
        buildAnnotatedString {
            words.forEachIndexed { index, word ->
                if (index > 0) append(" ")
                val active = !word.isEnd && playingWord != null && word.wordNumber == playingWord
                withStyle(
                    SpanStyle(
                        color = if (word.isEnd) colors.accent else colors.ink,
                        background = when {
                            active -> colors.accent.copy(alpha = 0.30f)
                            playingVerse -> colors.accent.copy(alpha = 0.10f)
                            else -> Color.Transparent
                        },
                    ),
                ) { append(word.code) }
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val available = with(density) { maxWidth.toPx() }
        val fontSp = remember(page, available, typeface, scale) {
            val paint = Paint().apply {
                this.typeface = typeface
                textSize = BASE_FONT_PX
            }
            val widest = page.lines.maxOfOrNull { line -> paint.measureText(line.words.joinToString(" ") { it.code }) } ?: 1f
            val target = if (page.pageNumber in setOf(1, 2)) available * 0.82f else available
            val px = (target / widest * BASE_FONT_PX).coerceIn(12f, 140f) * scale
            with(density) { px.toSp().value }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text,
                fontFamily = family,
                fontSize = fontSp.sp,
                lineHeight = (fontSp * lineHeightRatio).sp,
                textAlign = TextAlign.Start,
                onTextLayout = { layout.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onWordClick != null) {
                            Modifier.pointerInput(spans) {
                                detectTapGestures { position ->
                                    val result = layout.value ?: return@detectTapGestures
                                    val offset = result.getOffsetForPosition(position)
                                    val word = spans.firstOrNull { offset in it.first }?.second ?: return@detectTapGestures
                                    if (!word.isEnd) onWordClick(word.wordNumber)
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

/**
 * Oyat bo'yicha rejimdagi bitta oyat: raqam, bet / juz belgilari, tafsir belgisi, arabchasi
 * (mushaf shriftida), transkripsiya, tarjima va bosilganda ochiladigan panel.
 * [surahLink] — juz o'qishda: raqam "2:255" va sura nomi belgisi chiqadi.
 */
@Composable
fun AyahRow(
    ayah: Ayah,
    textMode: String,
    mushafPage: MushafPage?,
    extras: AyahExtras,
    onOpenPage: (Int) -> Unit,
    onOpenSurah: (Int) -> Unit,
    modifier: Modifier = Modifier,
    surahLink: Boolean = false,
    audio: AyahAudio? = null,
) {
    val colors = HolyTheme.colors
    val showArabic = textMode != "translation"
    val showTranslation = textMode != "arabic"
    var open by rememberSaveable(ayah.verseKey) { mutableStateOf(false) }
    val tafsir = if (showTranslation) extras.tafsir[ayah.verseKey] else null
    val saved = ayah.verseKey in extras.saved
    val playing = audio != null && audio.playingVerse == ayah.verseKey

    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)) {
        FlowRow(
            Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .heightIn(min = 28.dp)
                    .widthIn(min = 28.dp)
                    .clip(HolyTheme.shape)
                    .background(colors.surface2)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (surahLink) "${ayah.surahNumber}:${ayah.numberInSurah}" else "${ayah.numberInSurah}",
                    color = colors.inkSoft,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (surahLink && ayah.surahNameUz != null) {
                HolyBadge(ayah.surahNameUz, onClick = { onOpenSurah(ayah.surahNumber) })
            }
            if (!surahLink && ayah.pageNumber != null) {
                HolyBadge("${ayah.pageNumber}-sahifa", onClick = { onOpenPage(ayah.pageNumber) })
            }
            if (ayah.juz != null) HolyBadge("${ayah.juz}-juz")
            if (tafsir != null) HolyBadge("Tafsir", tone = BadgeTone.Accent, onClick = { open = !open })
            if (audio != null && audio.isReady(ayah.surahNumber)) {
                AyahAudioButtons(
                    isPlaying = playing && audio.isPlaying,
                    onPlay = { audio.onPlay(ayah) },
                    onPause = audio.onPause,
                    onStop = audio.onStop,
                )
            }
        }

        if (showArabic) {
            MushafAyah(
                ayah = ayah,
                page = mushafPage,
                playingVerse = playing,
                playingWord = if (playing) audio?.playingWord else null,
                onWordClick = if (audio != null && audio.isReady(ayah.surahNumber)) ({ word -> audio.onWord(ayah, word) }) else null,
            )
        }

        if (showTranslation && !ayah.transcription.isNullOrBlank()) {
            Text(
                ayah.transcription,
                style = ReadingText.uzSub.copy(fontStyle = FontStyle.Italic),
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        if (showTranslation) {
            if (!ayah.translationUz.isNullOrBlank()) {
                Text(
                    ayah.translationUz,
                    style = ReadingText.uz.copy(color = if (saved) colors.accent else colors.inkSoft),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .clip(HolyTheme.shape)
                        .clickable { open = !open },
                )
                AyahPanel(
                    open = open,
                    saved = saved,
                    onToggleSave = { extras.onToggleSave(ayah) },
                    tafsir = tafsir,
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                Text(
                    "Tarjima hali kiritilmagan",
                    style = ReadingText.uzSub.copy(fontStyle = FontStyle.Italic),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        }
    }
}

/* ---------------- Sura ma'lumoti ---------------- */

/**
 * Sura haqida qisqa ma'lumot: qayerda nozil bo'lgani, oyatlar soni, mushafdagi o'rni.
 * Betlar va juzlar Madina mushafi jadvallaridan olinadi.
 */
fun surahInfoText(surah: Surah): String? {
    val range = JuzCatalog.surahPageRange(surah.number) ?: return null
    val place = when (surah.revelationPlace) {
        "makka" -> "Makkada"
        "madina" -> "Madinada"
        else -> null
    }
    val total = surah.totalAyahs
    val pages = if (range.first == range.last) {
        "${range.first}-sahifasida"
    } else {
        "${range.first}–${range.last}-sahifalarida (${range.last - range.first + 1} bet)"
    }
    val firstJuz = JuzCatalog.juzOfAyah(surah.number, 1)
    val lastJuz = JuzCatalog.juzOfAyah(surah.number, total)
    val juz = when {
        firstJuz == null || lastJuz == null -> ""
        firstJuz == lastJuz -> ", $firstJuz-juzda"
        else -> ", $firstJuz–$lastJuz-juzlarda"
    }
    return "${if (place != null) "$place nozil boʻlgan, " else ""}$total oyatdan iborat. Mushafning $pages$juz joylashgan."
}

/** Sura sarlavhasi: kitobdagi nomi, ma'nosi, qisqa ma'lumot va belgilar (atrofidagi kartani chaqiruvchi beradi) */
@Composable
fun SurahInfo(surah: Surah, readPages: Map<Int, Long>, modifier: Modifier = Modifier, titleSize: Int = 24) {
    val colors = HolyTheme.colors
    val range = JuzCatalog.surahPageRange(surah.number)
    val status = range?.let { r -> ReadStatus(r.count { readPages.containsKey(it) }, r.count()) }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val wide = maxWidth >= 600.dp
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (wide) {
                Box(
                    Modifier.height(64.dp).widthIn(min = 64.dp).clip(HolyTheme.shape).background(colors.surface2).padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SurahGlyph(surah.number, fontSize = 32, color = colors.accent)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "${surah.number}. ${surah.nameUz}",
                    color = colors.ink,
                    fontSize = titleSize.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    lineHeight = (titleSize + 6).sp,
                )
                if (surah.meaningUz != null) {
                    Text(surah.meaningUz, color = colors.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                }
                surahInfoText(surah)?.let {
                    Text(it, color = colors.inkFaint, fontSize = 13.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
                }
                FlowRow(
                    Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (surah.revelationPlace != null) HolyBadge(if (surah.isMakki) "Makkiy" else "Madaniy")
                    HolyBadge("${surah.totalAyahs} oyat", tone = BadgeTone.Accent)
                    HolyBadge("${surah.startPage}-sahifadan")
                    if (status != null && range != null) {
                        HolyBadge(
                            if (status.done) {
                                "✓ Toʻliq oʻqildi · ${status.total} bet"
                            } else {
                                "${status.read}/${status.total} bet oʻqildi · ${status.total - status.read} ta qoldi"
                            },
                            tone = BadgeTone.Read,
                        )
                    }
                }
            }
        }
    }
}

/** Kitobdagi sura sarlavhasi xatti (KFGQPC surah-names): "003" → «آل عمران» */
@Composable
fun SurahGlyph(number: Int, fontSize: Int, color: Color, modifier: Modifier = Modifier, lineHeight: Int? = null) {
    val context = LocalContext.current
    val family = remember { HolyFonts.surahNames(context.assets) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            number.toString().padStart(3, '0'),
            color = color,
            fontFamily = family,
            fontSize = fontSize.sp,
            lineHeight = (lineHeight ?: fontSize).sp,
            modifier = modifier,
        )
    }
}
