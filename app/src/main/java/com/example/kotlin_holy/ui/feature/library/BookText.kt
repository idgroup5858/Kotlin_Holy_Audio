package com.example.kotlin_holy.ui.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.domain.model.Book
import com.example.kotlin_holy.domain.model.BookBlock
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.ReadToggle
import com.example.kotlin_holy.ui.components.formatReadAt
import com.example.kotlin_holy.ui.theme.HolyFonts
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ReadingText
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/* Oyat kartochkalari va ismlar bezagi (TanwirText) qo'llanadigan kitoblar */
private val RICH_TEXT_BOOKS = setOf("tanwir", "tahajjud")

/* ---------------- Bo'laklarga havolalar (Book.jsx → ItemLinks) ---------------- */

/* «250-hikmatga qarang», «(99, 176–178-hikmatlar)», «30 va 31-hikmatlar» — itemName bo'yicha */
private val itemPatterns = ConcurrentHashMap<String, Regex>()

private fun itemPattern(itemName: String): Regex = itemPatterns.getOrPut(itemName) {
    Regex("""\d+(?:–\d+)?(?:(?:,\s*|\s+va\s+)\d+(?:–\d+)?)*(?=-${Regex.escape(itemName)})""")
}

private val DIGITS = Regex("""\d+""")

/**
 * Matndagi havolalarning har bir raqami o'sha bo'lakni ochadi. Raqam kitobda bo'lmasa — oddiy matn.
 */
private fun AnnotatedString.Builder.appendItemLinks(
    text: String,
    book: Book,
    styles: TextLinkStyles,
    onItem: (Int) -> Unit,
) {
    var last = 0
    for (match in itemPattern(book.itemName).findAll(text)) {
        append(text, last, match.range.first)
        val piece = match.value
        var position = 0
        for (digits in DIGITS.findAll(piece)) {
            append(piece, position, digits.range.first)
            val number = digits.value.toIntOrNull()
            if (number != null && number in 1..book.totalItems) {
                withLink(LinkAnnotation.Clickable(tag = bookItemKey(book.id, number), styles = styles) { onItem(number) }) {
                    append(digits.value)
                }
            } else {
                append(digits.value)
            }
            position = digits.range.last + 1
        }
        append(piece, position, piece.length)
        last = match.range.last + 1
    }
    append(text, last, text.length)
}

/* ---------------- TanwirText.jsx ---------------- */

/*
 * «At-Tanvir fi isqot at-tadbir» va «Tahajjud va uni osonlashtiruvchi sabablar» matni uchun bezak:
 *   - «oyat» (Sura, 14) yoki „oyat“ (Sura, 3–4) — tafsir paneli uslubida alohida kartochka;
 *   - Alloh, Rasululloh, Paygʻambar, Muhammad (sollallohu alayhi vasallam bilan birga) — qalin.
 */
private const val REF = """\(([\p{L}ʼʻ' ]{2,20}),\s*(\d+(?:\s*[–-]\s*\d+)?)\)"""
private val AYAH = Regex("""«([^«»]+)»\s*$REF|„([^„“]+)“\s*$REF""")

private val NAMES = Regex(
    """(?<!\p{L})(?:Rasululloh\p{L}*|Paygʻambar(?!lar)\p{L}*|(?<!(?:ibn|Abu) )Muhammad(?! ibn)\p{L}*|Alloh\p{L}*(?: taolo\p{L}*)?)(?: sollallohu alayhi va ?sallam\p{L}*| alayhissalotu vassalom)?""",
)

private val LEADING_PUNCTUATION = Regex("""^[\s.,;:]*""")
private val WHITESPACE = Regex("""\s+""")
private val RANGE_DASH = Regex("""[–-]""")

/** Matn bo'lagi: oddiy matn yoki oyat kartochkasi */
private sealed interface RichPart

private data class PlainPart(val text: String) : RichPart

private data class AyahPart(val text: String, val surah: String, val ayahs: String) : RichPart {
    val label: String get() = if (RANGE_DASH.containsMatchIn(ayahs)) "$ayahs-oyatlar" else "$ayahs-oyat"
}

private fun richParts(text: String): List<RichPart> {
    val parts = mutableListOf<RichPart>()
    var last = 0
    for (match in AYAH.findAll(text)) {
        if (match.range.first > last) {
            val before = text.substring(last, match.range.first).trimEnd()
            if (before.isNotEmpty()) parts += PlainPart(before)
        }
        parts += AyahPart(
            text = match.groups[1]?.value ?: match.groups[4]?.value.orEmpty(),
            surah = match.groups[2]?.value ?: match.groups[5]?.value.orEmpty(),
            ayahs = (match.groups[3]?.value ?: match.groups[6]?.value.orEmpty()).replace(WHITESPACE, ""),
        )
        last = match.range.last + 1
        /* Kartochkadan keyingi matn tinish belgisi bilan boshlanmasin */
        last += LEADING_PUNCTUATION.find(text.substring(last))?.value?.length ?: 0
    }
    if (last < text.length) parts += PlainPart(text.substring(last))
    return parts
}

/** Ismlar qalin, qolgan matndagi raqamlar — havola */
private fun AnnotatedString.Builder.appendNames(
    text: String,
    book: Book,
    styles: TextLinkStyles,
    nameStyle: SpanStyle,
    onItem: (Int) -> Unit,
) {
    var last = 0
    for (match in NAMES.findAll(text)) {
        appendItemLinks(text.substring(last, match.range.first), book, styles, onItem)
        withStyle(nameStyle) { append(match.value) }
        last = match.range.last + 1
    }
    appendItemLinks(text.substring(last), book, styles, onItem)
}

/* ---------------- LinkedText ---------------- */

/** Havola ko'rinishi: text-accent font-semibold (bosilganda tagiga chiziladi) */
@Composable
private fun linkStyles(): TextLinkStyles {
    val accent = HolyTheme.colors.accent
    return remember(accent) {
        val base = SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)
        TextLinkStyles(style = base, pressedStyle = base.copy(textDecoration = TextDecoration.Underline))
    }
}

/**
 * Matndagi «250-hikmatga qarang», «(99, 176–178-hikmatlar)» kabi havolalarning har bir raqami
 * o'sha bo'lakni ochadi ([onItem]). «At-Tanvir» va «Tahajjud»da — oyat kartochkalari va qalin ismlar.
 */
@Composable
internal fun LinkedText(
    text: String,
    book: Book,
    style: TextStyle,
    onItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val styles = linkStyles()

    if (book.id !in RICH_TEXT_BOOKS) {
        val annotated = remember(text, book, styles, onItem) {
            buildAnnotatedString { appendItemLinks(text, book, styles, onItem) }
        }
        Text(annotated, modifier, style = style)
        return
    }

    val nameStyle = remember(colors.ink) { SpanStyle(color = colors.ink, fontWeight = FontWeight.Bold) }
    val parts = remember(text) { richParts(text) }
    val annotated = remember(parts, book, styles, nameStyle, onItem) {
        parts.map { part ->
            val body = when (part) {
                is PlainPart -> part.text
                is AyahPart -> part.text
            }
            buildAnnotatedString { appendNames(body, book, styles, nameStyle, onItem) }
        }
    }

    if (parts.size == 1 && parts[0] is PlainPart) {
        Text(annotated[0], modifier, style = style)
        return
    }
    Column(modifier.fillMaxWidth()) {
        parts.forEachIndexed { index, part ->
            when (part) {
                is PlainPart -> Text(annotated[index], style = style)
                is AyahPart -> AyahCard(
                    part = part,
                    text = annotated[index],
                    /* my-3 — xatboshi boshida va oxirida bloklar orasidagi oraliqqa qo'shilib ketadi */
                    modifier = Modifier.padding(
                        top = if (index > 0) 12.dp else 0.dp,
                        bottom = if (index < parts.lastIndex) 12.dp else 0.dp,
                    ),
                )
            }
        }
    }
}

/** Oyat: chap tomonida asosiy rangli chiziq, ustida «Qurʼon · Niso surasi, 65-oyat» */
@Composable
private fun AyahCard(part: AyahPart, text: AnnotatedString, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val radius = HolyTheme.radius
    val sub = ReadingText.uzSub
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = radius, bottomEnd = radius))
            .background(colors.surface2)
            .drawBehind { drawRect(colors.accent, size = Size(3.dp.toPx(), size.height)) }
            .padding(start = 19.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
    ) {
        Text(
            "Qurʼon · ${part.surah} surasi, ${part.label}",
            color = colors.inkFaint,
            fontSize = 11.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        Text(text, style = sub.copy(color = colors.ink))
    }
}

/* ---------------- BookBlocks ---------------- */

/**
 * Kitob matni (Muqaddima, Atamalar lug'ati, bob): bloklar ketma-ketligi — har bir blok
 * ro'yxatning alohida qatori (uzun boblar ham silliq suriladi).
 * { type: "section" | "h" | "p" | "quote" | "arabic", text }; { type: "list", ordered?, start?, items } —
 * band matn yoki { text, items }; { type: "terms", items: [{ term, text }] }.
 * Matndagi "\n" — paragraf ichidagi yangi qator, quote'da — sheʼr misrasi (bo'sh qator — baytlar orasi).
 */
internal fun LazyListScope.bookBlocks(
    blocks: List<BookBlock>,
    book: Book,
    wide: Boolean,
    onItem: (Int) -> Unit,
    keyPrefix: String,
) {
    itemsIndexed(
        blocks,
        key = { index, _ -> "$keyPrefix:$index" },
        contentType = { _, block -> block.type },
    ) { index, block ->
        /* space-y-4; asosiy qism (section) oldida — mt-8 */
        val top = when {
            index == 0 -> 0.dp
            block.type == "section" -> 32.dp
            else -> 16.dp
        }
        BookBlockView(
            block = block,
            book = book,
            first = index == 0,
            wide = wide,
            onItem = onItem,
            modifier = Modifier.padding(top = top),
        )
    }
}

@Composable
private fun BookBlockView(
    block: BookBlock,
    book: Book,
    first: Boolean,
    wide: Boolean,
    onItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val text = block.text.orEmpty()
    when (block.type) {
        "section" -> Column(modifier.fillMaxWidth()) {
            if (!first) {
                Divider()
                Spacer(Modifier.height(24.dp))
            }
            Text(
                text.uppercase(Locale.ROOT),
                color = colors.accent,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.12.em,
            )
        }

        "h" -> Text(
            text,
            style = ReadingText.uzHeading.copy(letterSpacing = (-0.025).em),
            modifier = modifier.fillMaxWidth().padding(top = 12.dp),
        )

        /* Sheʼr: har qator — alohida misra; sigʻmagan misraning davomi ichkariroqdan boshlanadi.
           Misralar kenglik bo'ylab cho'zilmaydi — chapga tekis. Bo'sh qator — baytlar orasidagi bo'shliq */
        "quote" -> {
            val line = colors.accent.copy(alpha = 0.6f)
            val style = ReadingText.uz.copy(
                color = colors.ink,
                textAlign = TextAlign.Left,
                textIndent = TextIndent(firstLine = 0.sp, restLine = 20.sp),
            )
            Column(
                modifier
                    .fillMaxWidth()
                    .drawBehind { drawRect(line, size = Size(2.dp.toPx(), size.height)) }
                    .padding(start = 18.dp),
            ) {
                text.split('\n').forEach { verse ->
                    if (verse.isNotEmpty()) {
                        LinkedText(verse, book, style, onItem)
                    } else {
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        /* Oyat yoki duoning arabcha matni — markazda, arabcha matn o'lchami sozlamasiga bog'liq */
        "arabic" -> {
            val assets = LocalContext.current.assets
            val family = remember(assets) { HolyFonts.uthmani(assets) }
            /* .txt-arabic: 27px, sm (640px) dan boshlab 30px */
            Text(
                text,
                style = ReadingText.arabic(family, baseSize = if (wide) 30f else 27f)
                    .copy(textAlign = TextAlign.Center),
                modifier = modifier.fillMaxWidth(),
            )
        }

        "list" -> {
            val style = ReadingText.uz
            Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                block.items.orEmpty().forEachIndexed { index, element ->
                    val marker = if (block.ordered) "${(block.start ?: 1) + index}." else "•"
                    ListItemView(element, marker, style, book, onItem)
                }
            }
        }

        "terms" -> HolyCard(modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                block.items.orEmpty().forEachIndexed { index, element ->
                    val entry = element as? JsonObject ?: return@forEachIndexed
                    val term = entry.string("term")
                    val definition = entry.string("text")
                    if (index > 0) Divider()
                    TermRow(term, definition, wide, book, onItem)
                }
            }
        }

        else -> LinkedText(text, book, ReadingText.uz, onItem, modifier.fillMaxWidth())
    }
}

/** Atamalar lug'ati qatori: tor ekranda atama ustida, kengda — chapda 10rem ustunda */
@Composable
private fun TermRow(term: String, definition: String, wide: Boolean, book: Book, onItem: (Int) -> Unit) {
    val colors = HolyTheme.colors
    val style = ReadingText.uzSub.copy(color = colors.inkSoft)
    val termText: @Composable (Modifier) -> Unit = { modifier ->
        Text(term, color = colors.ink, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold, modifier = modifier)
    }
    if (wide) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            termText(Modifier.width(160.dp))
            LinkedText(definition, book, style, onItem, Modifier.weight(1f))
        }
    } else {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            termText(Modifier)
            LinkedText(definition, book, style, onItem)
        }
    }
}

/** Ro'yxat bandi: matn yoki { text, items } — ichki ro'yxati bor band */
@Composable
private fun ListItemView(
    element: JsonElement,
    marker: String,
    style: TextStyle,
    book: Book,
    onItem: (Int) -> Unit,
) {
    when (element) {
        is JsonPrimitive -> MarkerRow(marker, style) {
            LinkedText(element.content, book, style, onItem)
        }

        is JsonObject -> MarkerRow(marker, style) {
            Column(Modifier.fillMaxWidth()) {
                LinkedText(element.string("text"), book, style, onItem)
                val nested = (element["items"] as? JsonArray).orEmpty()
                if (nested.isNotEmpty()) {
                    /* ul.list-[circle] mt-1 space-y-1 pl-6 — belgi o'sha pl-6 ichida turadi */
                    Column(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        nested.forEach { child ->
                            val childText = (child as? JsonPrimitive)?.content ?: (child as? JsonObject)?.string("text")
                            if (childText != null) {
                                MarkerRow("◦", style) {
                                    LinkedText(childText, book, style, onItem)
                                }
                            }
                        }
                    }
                }
            }
        }

        else -> Unit
    }
}

/** list-disc / list-decimal pl-6: belgi chapdagi 1.5rem ustunda o'ngga tekis, matn — pl-1 bilan */
@Composable
private fun MarkerRow(
    marker: String,
    style: TextStyle,
    content: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    Row(Modifier.fillMaxWidth()) {
        Text(
            marker,
            style = style.copy(color = colors.inkFaint, textAlign = TextAlign.End),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(24.dp).padding(end = 4.dp),
        )
        Box(Modifier.weight(1f).padding(start = 4.dp)) { content() }
    }
}

private fun JsonObject.string(name: String): String = (this[name] as? JsonPrimitive)?.content.orEmpty()

/* ---------------- BookItemReadToggle ---------------- */

/** Bitta bo'lakni (hikmatni) o'qildi deb belgilash — hadislardagi doiracha */
@Composable
internal fun BookItemReadToggle(
    book: Book,
    number: Int,
    readAt: Long?,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = book.itemLabel(number)
    ReadToggle(
        done = readAt != null,
        onToggle = { onToggle(number) },
        modifier = modifier,
        contentDescription = if (readAt != null) {
            "$label belgisini olib tashlash · Oʻqildi ${formatReadAt(readAt)}"
        } else {
            "${label}ni oʻqildi deb belgilash"
        },
    )
}
