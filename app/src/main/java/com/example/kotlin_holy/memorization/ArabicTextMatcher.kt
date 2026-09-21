package com.example.kotlin_holy.memorization

/** Tashkil (harakat, tanvin, sukun va h.k.) belgilari diapazoni */
private val DIACRITICS = Regex("[ؐ-ًؚ-ٟۖ-ۜ۟-۪ۨ-ٰۭ]")

/** Arab harflari va bo'shliqdan boshqa hamma narsa (tinish belgilari, oyat raqami va h.k.) */
private val NON_LETTERS = Regex("[^ء-ي\\s]")

/**
 * Tashkilni, harflarning kichik yozilish farqlarini (alif/hamza, ta marbuta, yo)
 * va tinish belgilarini olib tashlab, matnni solishtirishga tayyorlaydi. STT
 * natijasi ham, mushaf matni ham shu funksiyadan o'tgach taqqoslanadi.
 */
fun normalizeArabic(text: String): String {
    var result = DIACRITICS.replace(text, "")
    result = result
        .replace('آ', 'ا') // آ -> ا
        .replace('أ', 'ا') // أ -> ا
        .replace('إ', 'ا') // إ -> ا
        .replace('ٱ', 'ا') // ٱ -> ا
        .replace('ة', 'ه') // ة -> ه
        .replace('ى', 'ي') // ى -> ي
    result = NON_LETTERS.replace(result, " ")
    return result.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ")
}

data class MatchResult(
    val matchedWords: Int,
    val totalWords: Int,
    val correct: Boolean,
) {
    val ratio: Float get() = if (totalWords == 0) 0f else matchedWords.toFloat() / totalWords
}

/**
 * Tanilgan nutqni oyat matni bilan so'zma-so'z, tartib bo'yicha solishtiradi.
 * Nutqni tanish 100% aniq bo'lmagani uchun erkin taqqoslanadi: bitta-ikkita
 * harf farq qilsa ham so'z mos deb hisoblanadi, va umumiy so'zlarning kamida
 * [threshold] qismi mos kelsa oyat "to'g'ri o'qildi" deb belgilanadi.
 */
fun matchRecitation(expected: String, recognized: String, threshold: Float = 0.8f): MatchResult {
    val expectedWords = normalizeArabic(expected).split(" ").filter { it.isNotBlank() }
    val recognizedWords = normalizeArabic(recognized).split(" ").filter { it.isNotBlank() }
    if (expectedWords.isEmpty()) return MatchResult(0, 0, false)

    var matched = 0
    var searchFrom = 0
    for (word in expectedWords) {
        val found = (searchFrom until recognizedWords.size).firstOrNull { idx ->
            wordsClose(recognizedWords[idx], word)
        }
        if (found != null) {
            matched++
            searchFrom = found + 1
        }
    }

    val ratio = matched.toFloat() / expectedWords.size
    return MatchResult(matched, expectedWords.size, ratio >= threshold)
}

private fun wordsClose(a: String, b: String): Boolean {
    if (a == b) return true
    if (kotlin.math.abs(a.length - b.length) > 2) return false
    val maxDistance = if (b.length <= 3) 1 else 2
    return levenshtein(a, b) <= maxDistance
}

private fun levenshtein(a: String, b: String): Int {
    val dp = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) dp[i][0] = i
    for (j in 0..b.length) dp[0][j] = j
    for (i in 1..a.length) {
        for (j in 1..b.length) {
            dp[i][j] = if (a[i - 1] == b[j - 1]) {
                dp[i - 1][j - 1]
            } else {
                1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
            }
        }
    }
    return dp[a.length][b.length]
}
