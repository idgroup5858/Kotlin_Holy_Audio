package com.example.kotlin_holy.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.kotlin_holy.domain.model.MushafLine
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.domain.model.MushafWord
import com.example.kotlin_holy.ui.theme.HolyTheme

private const val BASE_FONT_PX = 100f

/* Satr chekkaga tegib turmasligi uchun kichik zaxira */
private const val SAFETY = 0.99f

/* 1- va 2-sahifalar bosma mushafda ham markazga tekislangan */
private val CENTERED_PAGES = setOf(1, 2)

/**
 * Mushaf beti.
 *
 * Bosma mushafda har bir satr o'z tabiiy bo'shliqlari bilan betning to'liq
 * enini egallaydi — shrift aynan shu bet uchun shunday chizilgan. Shuning
 * uchun satrlar yaxlit matn sifatida chiziladi, so'zlar sun'iy ravishda
 * bir-biridan uzoqlashtirilmaydi. Shrift o'lchami esa eng uzun satr
 * sig'adigan qilib tanlanadi.
 *
 * Qiroat qo'shilgach ham shu qoida buzilmadi: na uch nuqta belgisi, na
 * boshqaruv tugmalari matn ichiga kiritiladi. Ular matn ustiga qatlam bo'lib
 * chiziladi, shuning uchun satr eni va shrift o'lchami o'zgarmaydi.
 */
@Composable
fun MushafPageView(
    page: MushafPage,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    highlightedVerse: String? = null,
    /** Hozir o'qilayotgan oyat */
    playingVerse: String? = null,
    /** Hozir o'qilayotgan so'zning oyat ichidagi raqami */
    playingWord: Int? = null,
    /** Boshqaruv tugmalari ochilgan oyat — oyat raqami bosilganda to'ladi */
    menuVerse: String? = null,
    isPlaying: Boolean = false,
    /** Qiroat yuklanmagan bo'lsa tugmalar va so'z bosish ishlamaydi */
    audioReady: Boolean = false,
    onVerseClick: (String) -> Unit = {},
    onWordClick: (surah: Int, ayah: Int, word: Int) -> Unit = { _, _, _ -> },
    onMenuOpen: (verseKey: String) -> Unit = {},
    onMenuDismiss: () -> Unit = {},
    onPlay: (surah: Int, ayah: Int) -> Unit = { _, _ -> },
    onPlayFrom: (surah: Int, ayah: Int) -> Unit = { _, _ -> },
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
) {
    val (family, typeface) = rememberMushafFont(page.pageNumber)
    val centered = page.pageNumber in CENTERED_PAGES
    val rows = remember(page) { buildMushafRows(page, centered) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val availablePx = with(density) { maxWidth.toPx() }

        val fontSizeSp = remember(page, availablePx, typeface, scale) {
            val widest = page.lines.maxOfOrNull { it.naturalWidth(typeface) } ?: 1f
            val target = (if (centered) availablePx * 0.86f else availablePx) * SAFETY
            val fontPx = (target / widest * BASE_FONT_PX).coerceIn(10f, 200f)
            with(density) { fontPx.toSp().value } * scale
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(Modifier.fillMaxWidth()) {
                rows.forEach { row ->
                    when (row) {
                        is MushafRow.Header -> SurahHeaderOrnament(
                            surahNumber = row.surahNumber,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )

                        MushafRow.Basmala -> BasmalaRow(fontSizeSp = fontSizeSp)

                        is MushafRow.Line -> MushafTextLine(
                            line = row.line,
                            family = family,
                            fontSizeSp = fontSizeSp,
                            highlightedVerse = highlightedVerse,
                            playingVerse = playingVerse,
                            playingWord = playingWord,
                            menuVerse = menuVerse,
                            isPlaying = isPlaying,
                            audioReady = audioReady,
                            onVerseClick = onVerseClick,
                            onWordClick = onWordClick,
                            onMenuOpen = onMenuOpen,
                            onMenuDismiss = onMenuDismiss,
                            onPlay = onPlay,
                            onPlayFrom = onPlayFrom,
                            onPause = onPause,
                            onStop = onStop,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Satrning tabiiy eni. Bo'shliq belgisi qo'shilmaydi: mushaf shriftida har bir
 * glifning o'zida so'z orasidagi masofa hisobga olingan. Shu sababli betdagi
 * barcha satrlar deyarli bir xil enga ega bo'ladi (tekshirildi: 3-betda
 * 1340–1350 birlik), ortiqcha bo'shliq esa so'zlarni uzib ko'rsatadi.
 */
private fun MushafLine.naturalWidth(typeface: Typeface): Float {
    val paint = Paint().apply {
        this.typeface = typeface
        textSize = BASE_FONT_PX
        isAntiAlias = true
    }
    return paint.measureText(words.joinToString("") { it.code })
}

/** So'z matnning qaysi belgilar oralig'ida turishi */
private data class WordSpan(val range: IntRange, val word: MushafWord)

@Composable
private fun MushafTextLine(
    line: MushafLine,
    family: FontFamily,
    fontSizeSp: Float,
    highlightedVerse: String?,
    playingVerse: String?,
    playingWord: Int?,
    menuVerse: String?,
    isPlaying: Boolean,
    audioReady: Boolean,
    onVerseClick: (String) -> Unit,
    onWordClick: (surah: Int, ayah: Int, word: Int) -> Unit,
    onMenuOpen: (verseKey: String) -> Unit,
    onMenuDismiss: () -> Unit,
    onPlay: (surah: Int, ayah: Int) -> Unit,
    onPlayFrom: (surah: Int, ayah: Int) -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = HolyTheme.colors
    val layout = remember(line) { mutableStateOf<TextLayoutResult?>(null) }

    /* Har bir so'z matnning qaysi qismida turishini eslab qolamiz — bosilganda
       qaysi so'z va qaysi oyat tanlanganini shu orqali topamiz */
    val spans = remember(line) {
        var start = 0
        line.words.map { word ->
            val span = WordSpan(start until (start + word.code.length), word)
            start += word.code.length
            span
        }
    }

    /*
     * Matn faqat yonish o'zgarganda qayta quriladi. Ijro paytida so'z almashsa
     * shu satrgina qayta chiziladi, butun bet emas — shuning uchun qiroat
     * davomida ham ilova silliq ishlaydi.
     */
    val text: AnnotatedString = remember(line, highlightedVerse, playingVerse, playingWord, colors) {
        buildAnnotatedString {
            line.words.forEach { word ->
                val onPlayingVerse = playingVerse != null && word.verseKey == playingVerse
                val isPlayingWord = onPlayingVerse && !word.isEnd &&
                    playingWord != null && word.wordNumber == playingWord
                val selected = highlightedVerse != null && word.verseKey == highlightedVerse

                val background = when {
                    isPlayingWord -> colors.accent.copy(alpha = 0.30f)
                    onPlayingVerse -> colors.accent.copy(alpha = 0.10f)
                    selected -> colors.surface2
                    else -> Color.Transparent
                }

                withStyle(
                    SpanStyle(
                        color = if (word.isEnd) colors.accent else colors.ink,
                        background = background,
                    ),
                ) {
                    append(word.code)
                }
            }
        }
    }

    /* Tanlangan oyatning raqami ustiga uch nuqta chiziladi */
    val markerSpan = spans.firstOrNull { it.word.isEnd && it.word.verseKey == highlightedVerse }
    val menuSpan = spans.firstOrNull { it.word.isEnd && it.word.verseKey == menuVerse }

    Box(Modifier.fillMaxWidth()) {
        Text(
            text = text,
            fontFamily = family,
            fontSize = fontSizeSp.sp,
            lineHeight = (fontSizeSp * 1.85f).sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            onTextLayout = { layout.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(spans, audioReady) {
                    detectTapGestures { position ->
                        val result = layout.value ?: return@detectTapGestures
                        val offset = result.getOffsetForPosition(position)
                        val hit = spans.firstOrNull { offset in it.range }
                            ?: return@detectTapGestures
                        val word = hit.word

                        onVerseClick(word.verseKey)
                        if (word.isEnd) {
                            /* Oyat raqami — shu yerning o'zida boshqaruv chiqadi */
                            onMenuOpen(word.verseKey)
                        } else if (audioReady) {
                            /* So'z — o'sha so'z o'qiladi */
                            onWordClick(word.surahNumber, word.ayahNumber, word.wordNumber)
                        }
                    }
                },
        )

        /* Uch nuqta: tanlangan oyatning raqami ostida turadi.
           absoluteOffset ishlatiladi, chunki oddiy offset RTL matnda
           gorizontal o'qni teskari aylantirib yuboradi. */
        val markerBox = markerSpan?.let { span ->
            layout.value?.let { runCatching { it.getBoundingBox(span.range.first) }.getOrNull() }
        }
        if (markerSpan != null && markerBox != null && menuSpan == null) {
            Box(
                modifier = Modifier
                    .absoluteOffset {
                        IntOffset(
                            (markerBox.center.x - MARKER_SIZE.toPx() / 2f).toInt(),
                            markerBox.bottom.toInt(),
                        )
                    }
                    .size(MARKER_SIZE)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.16f))
                    .clickable { onMenuOpen(markerSpan.word.verseKey) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Oyat boshqaruvi",
                    tint = colors.accent,
                    modifier = Modifier.size(15.dp),
                )
            }
        }

        /* Boshqaruv tugmalari — aynan oyat raqami turgan joyda ochiladi */
        val menuBox = menuSpan?.let { span ->
            layout.value?.let { runCatching { it.getBoundingBox(span.range.first) }.getOrNull() }
        }
        if (menuSpan != null && menuBox != null) {
            val anchorX = menuBox.center.x.toInt()
            val anchorY = menuBox.bottom.toInt()
            Popup(
                popupPositionProvider = AyahMenuPosition(anchorX, anchorY),
                onDismissRequest = onMenuDismiss,
                properties = PopupProperties(focusable = true),
            ) {
                AyahInlineControls(
                    ayahNumber = menuSpan.word.ayahNumber,
                    isPlaying = isPlaying,
                    enabled = audioReady,
                    onPlay = { onPlay(menuSpan.word.surahNumber, menuSpan.word.ayahNumber) },
                    onPlayFrom = { onPlayFrom(menuSpan.word.surahNumber, menuSpan.word.ayahNumber) },
                    onPause = onPause,
                    onStop = onStop,
                )
            }
        }
    }
}

/**
 * Boshqaruv oynasini oyat raqamining ostiga qo'yadi va ekran chetidan
 * chiqib ketmasligini ta'minlaydi. Joylashuv qo'lda hisoblanadi — shunda
 * matnning o'ngdan chapga yozilishi natijaga ta'sir qilmaydi.
 */
private class AyahMenuPosition(
    private val anchorX: Int,
    private val anchorY: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = anchorBounds.left + anchorX - popupContentSize.width / 2
        val y = anchorBounds.top + anchorY + GAP_PX
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }

    private companion object {
        const val GAP_PX = 4
    }
}

/** Oyat raqami yonida chiqadigan kichik boshqaruv: o'qish, to'xtatish, davomi */
@Composable
private fun AyahInlineControls(
    ayahNumber: Int,
    isPlaying: Boolean,
    enabled: Boolean,
    onPlay: () -> Unit,
    onPlayFrom: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = HolyTheme.colors

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(colors.surface)
                .border(1.dp, colors.accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "$ayahNumber",
                color = colors.inkFaint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            if (!enabled) {
                Text(
                    text = "qiroat yuklanmagan",
                    color = colors.inkFaint,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                return@Row
            }

            if (isPlaying) {
                MenuButton(Icons.Filled.Pause, "Toʻxtatib turish", onPause)
            } else {
                MenuButton(Icons.Filled.PlayArrow, "Oyatni oʻqish", onPlay, primary = true)
            }
            MenuButton(Icons.Filled.Stop, "Toʻxtatish", onStop)

            Text(
                text = "Davomi",
                color = colors.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onPlayFrom)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun MenuButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    primary: Boolean = false,
) {
    val colors = HolyTheme.colors
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(if (primary) colors.accent else colors.surface2)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (primary) colors.bg else colors.inkSoft,
            modifier = Modifier.size(17.dp),
        )
    }
}

private val MARKER_SIZE = 20.dp
