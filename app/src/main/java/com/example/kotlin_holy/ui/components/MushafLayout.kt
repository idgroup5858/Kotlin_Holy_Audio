package com.example.kotlin_holy.ui.components

import com.example.kotlin_holy.domain.model.MushafLine
import com.example.kotlin_holy.domain.model.MushafPage

/** Mushaf betidagi bitta qator: matn satri, sura sarlavhasi yoki basmala */
sealed interface MushafRow {
    data class Line(val line: MushafLine) : MushafRow
    data class Header(val surahNumber: Int) : MushafRow
    data object Basmala : MushafRow
}

private const val LINES_PER_PAGE = 15

/**
 * Mushaf JSON'ida sura sarlavhasi va basmala satrlari yo'q — ularning o'rnida
 * satr raqamlari tushib qolgan. Shu bo'shliqlarni topib, har biriga nima
 * qo'yilishini aniqlaymiz (veb nusxadagi mantiqning aynan o'zi).
 */
fun buildMushafRows(page: MushafPage, centered: Boolean): List<MushafRow> {
    val byNumber = page.lines.associateBy { it.lineNumber }
    val lastLine = page.lines.lastOrNull()?.lineNumber ?: 0
    val total = if (centered) lastLine else maxOf(LINES_PER_PAGE, lastLine)
    val rows = mutableListOf<MushafRow>()

    var number = 1
    while (number <= total) {
        val line = byNumber[number]
        if (line != null) {
            rows += MushafRow.Line(line)
            number++
            continue
        }

        var end = number
        while (end < total && !byNumber.containsKey(end + 1)) end++
        val length = end - number + 1
        val next = page.lines.firstOrNull { it.lineNumber > end }

        if (next == null) {
            /* Bet oxirida faqat keyingi suraning sarlavhasi turadi, basmala keyingi betda */
            val surah = (page.lines.lastOrNull()?.words?.lastOrNull()?.surahNumber ?: 0) + 1
            if (surah <= 114) rows += MushafRow.Header(surah)
        } else {
            val surah = next.words.first().surahNumber
            /* Fotihada basmala 1-oyatning o'zi, Tavbada esa basmala yo'q */
            val hasBasmala = surah != 1 && surah != 9
            if (length == 1 && number == 1 && hasBasmala) {
                /* Sarlavha oldingi betning oxirida qolgan */
                rows += MushafRow.Basmala
            } else {
                rows += MushafRow.Header(surah)
                if (length >= 2 && hasBasmala) rows += MushafRow.Basmala
            }
        }
        number = end + 1
    }

    return rows
}

/** Satr sura oxiri bo'lsa, bosma mushafda cho'zilmaydi — markazda turadi */
fun endsSurah(page: MushafPage, index: Int): Boolean {
    val line = page.lines.getOrNull(index) ?: return false
    val next = page.lines.getOrNull(index + 1) ?: return false
    return next.words.first().surahNumber != line.words.last().surahNumber
}
