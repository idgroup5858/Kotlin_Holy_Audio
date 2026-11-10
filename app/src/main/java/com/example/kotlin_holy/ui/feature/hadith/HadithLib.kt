package com.example.kotlin_holy.ui.feature.hadith

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import com.example.kotlin_holy.domain.model.numberLabel
import com.example.kotlin_holy.domain.repository.LibraryRepository
import com.example.kotlin_holy.ui.components.ReadStatus
import com.example.kotlin_holy.ui.theme.AmiriQuran
import com.example.kotlin_holy.ui.theme.HolyFonts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

/*
 * Veb nusxadagi lib/hadith.js va lib/hadithMarks.js: rivoyatchi nomi, qisqa mazmun,
 * o'qildi belgilari bo'yicha bo'lim / kitob holati, fayllarni yuklash.
 */

/* ---------------- lib/hadith.js ---------------- */

internal val GRADES = mapOf(
    "sahih" to "Sahih",
    "hasan" to "Hasan",
    "hasan-sahih" to "Hasan sahih",
)

/** Hadis darajasi yorlig'i: «Sahih», «Hasan sahih» */
internal fun gradeLabel(grade: String): String = GRADES[grade] ?: grade

/* "Rasululloh sollallohu alayhi va sallamning nevaralari va rayhonlari (sevimlilari) Abu Muhammad…" kabi
   izoh qismi: kichik harfli so'zlar / qavsli izohlar, undan keyin bosh harfli ism boshlanadi */
private val PROPHET_ATTRIBUTION =
    Regex("^Rasululloh sollallohu alayhi va sallamning\\s+(?:(?:\\([^)]*\\)|[a-zʻʼ]+)\\s+)+(?=[A-Z])")
private val ISNAD_START = Regex("^(Menga|Bizga)\\s")
private val NARRATOR_END = Regex("\\s+roziyallohu\\b")
private val NARRATED_TAIL = Regex("\\s+rivoyat qil\\w*.*$")
private val ISNAD_CHAIN = Regex("dan, u ")

/**
 * "Abu Hurayra roziyallohu anhudan rivoyat qilinadi." → "Abu Hurayra".
 * To'liq isnod zanjiri bo'lsa ("Menga Molik Nofiʼdan, u …") bo'sh qaytadi — sarlavhaga yaramaydi.
 */
internal fun narratorName(narratorUz: String?): String {
    if (narratorUz.isNullOrEmpty() || ISNAD_START.containsMatchIn(narratorUz)) return ""
    val cut = NARRATOR_END.find(narratorUz)?.range?.first ?: -1
    val name = if (cut > 0) narratorUz.substring(0, cut) else NARRATED_TAIL.replaceFirst(narratorUz, "")
    if (ISNAD_CHAIN.containsMatchIn(name)) return ""
    return PROPHET_ATTRIBUTION.replaceFirst(name, "").trim()
}

private val QUOTES = Regex("[«»“”]")
private val SPACES = Regex("\\s+")
private val LAST_WORD = Regex("\\s+\\S*$")

/**
 * Ro'yxatdagi kartada ko'rsatiladigan qisqa mazmun: "Rasululloh ... dedilar: «" kabi
 * kirish qismi tashlanadi, qolgan matn maxLength gacha qirqiladi.
 */
internal fun hadithExcerpt(textUz: String, maxLength: Int = 150): String {
    val quote = textUz.indexOf('«')
    val body = if (quote in 0 until 120) textUz.substring(quote + 1) else textUz
    val clean = body.replace(QUOTES, "").replace(SPACES, " ").trim()
    if (clean.length <= maxLength) return clean
    return "${LAST_WORD.replaceFirst(clean.substring(0, maxLength), "")}…"
}

/* ---------------- lib/hadithMarks.js ---------------- */

/** O'qildi belgisi kaliti: «bukhari:12» — hadis raqamlari har bir kitob ichida yagona */
internal fun hadithKey(collectionId: String, number: Double) = "$collectionId:${numberLabel(number)}"

/** Kitobda belgilangan hadis raqamlari — o'sish tartibida (bo'limlar bo'yicha sanash uchun) */
internal fun markedNumbers(marks: Map<String, Long>, collectionId: String): DoubleArray {
    val prefix = "$collectionId:"
    return marks.keys
        .filter { it.startsWith(prefix) }
        .mapNotNull { it.substring(prefix.length).toDoubleOrNull() }
        .sorted()
        .toDoubleArray()
}

/* Tartiblangan massivda value dan kichik bo'lmagan birinchi element o'rni */
private fun lowerBound(sorted: DoubleArray, value: Double): Int {
    var low = 0
    var high = sorted.size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (sorted[middle] < value) low = middle + 1 else high = middle
    }
    return low
}

/**
 * Bo'lim holati. Bo'limning hadislari sections.json dagi ranges oraliqlarida — ular boshqa
 * bo'lim hadislarini o'z ichiga olmaydi (Termiziyda bo'limlar aralash).
 */
internal fun sectionReadStatus(numbers: DoubleArray, section: HadithSection): ReadStatus {
    val read = section.hadithRanges.sumOf { range ->
        lowerBound(numbers, range.endInclusive + 1) - lowerBound(numbers, range.start)
    }
    return ReadStatus(read, section.totalHadiths)
}

/** Butun kitob holati */
internal fun collectionReadStatus(marks: Map<String, Long>, collection: HadithCollection): ReadStatus {
    val prefix = "${collection.id}:"
    return ReadStatus(marks.keys.count { it.startsWith(prefix) }, collection.totalHadiths)
}

/* ---------------- Yuklash (veb nusxadagi useKeyed: { value, error, loading }) ---------------- */

internal sealed interface Loaded<out T> {
    data object Loading : Loaded<Nothing>
    data class Failed(val message: String) : Loaded<Nothing>
    data class Ready<T>(val value: T) : Loaded<T>
}

/** Kitob va uning bo'limlari ro'yxati */
internal data class BookMeta(val collection: HadithCollection, val sections: List<HadithSection>)

/** Xato matni veb nusxadagidek: «bukhari/section/99.json topilmadi» */
private fun errorMessage(error: Exception, path: String): String =
    if (error is IOException) "$path topilmadi" else error.message ?: "Nomaʼlum xatolik"

private inline fun <T> attempt(path: String, block: () -> T): Loaded<T> =
    try {
        Loaded.Ready(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Loaded.Failed(errorMessage(error, path))
    }

internal suspend fun loadCollections(library: LibraryRepository): Loaded<List<HadithCollection>> =
    attempt("collections.json") { library.collections() }

/** Kitob va bo'limlari; kitob topilmasa — «"x" kitobi topilmadi» */
internal suspend fun loadBookMeta(library: LibraryRepository, id: String): Loaded<BookMeta> {
    val collection = when (val result = attempt("collections.json") { library.collection(id) }) {
        is Loaded.Ready -> result.value ?: return Loaded.Failed("\"$id\" kitobi topilmadi")
        is Loaded.Failed -> return result
        Loaded.Loading -> return Loaded.Loading
    }
    return when (val sections = attempt("$id/sections.json") { library.sections(id) }) {
        is Loaded.Ready -> Loaded.Ready(BookMeta(collection, sections.value))
        is Loaded.Failed -> sections
        Loaded.Loading -> Loaded.Loading
    }
}

/** Bitta bo'limdagi hadislar */
internal suspend fun loadSectionHadiths(library: LibraryRepository, id: String, section: String): Loaded<List<Hadith>> {
    val path = "$id/section/$section.json"
    val number = section.toIntOrNull() ?: return Loaded.Failed("$path topilmadi")
    return attempt(path) { library.hadiths(id, number) }
}

/* ---------------- Arabcha matn: KFGQPC Hafs ---------------- */

/* Bitta nusxa — uzun ro'yxatdagi har bir qator shriftni qayta yaratmasligi uchun */
private var hafsFamily: FontFamily? = null

/** Veb nusxadagi .arabic-hadith shrifti */
@Composable
internal fun rememberHafs(): FontFamily {
    val assets = LocalContext.current.applicationContext.assets
    return remember(assets) { hafsFamily ?: HolyFonts.uthmani(assets).also { hafsFamily = it } }
}

/*
 * Veb nusxadagi @font-face unicode-range: arabcha vergul, nuqtali vergul va so'roq belgisi
 * (U+060C, U+061B, U+061F) bu shriftda Qur'on oyat ornamenti sifatida chizilgan — ular va
 * oraliqdan tashqaridagi belgilar zaxira shriftdan (Amiri Quran) olinadi.
 */
private fun inHafsRange(code: Int): Boolean =
    code == 0x20 || code in 0x30..0x39 || code == 0x5B || code == 0x5D ||
        code in 0x600..0x60B || code in 0x60D..0x61A || code in 0x61C..0x61E ||
        code in 0x620..0x6FF || code == 0x25CC

private val FALLBACK = SpanStyle(fontFamily = AmiriQuran)

/** Hadis arabchasi: Hafs oralig'idan tashqaridagi belgilar Amiri Quran'da. [limit] — qisqa ko'rinish uchun */
internal fun hafsText(text: String, limit: Int = Int.MAX_VALUE): AnnotatedString {
    val value = if (text.length > limit) text.substring(0, limit) else text
    return buildAnnotatedString {
        append(value)
        var start = -1
        value.forEachIndexed { index, char ->
            val fallback = !inHafsRange(char.code)
            if (fallback && start < 0) start = index
            if (!fallback && start >= 0) {
                addStyle(FALLBACK, start, index)
                start = -1
            }
        }
        if (start >= 0) addStyle(FALLBACK, start, value.length)
    }
}

/* ---------------- Ko'rinish yordamchilari ---------------- */

/** Ro'yxatdagi kartochka va qatorlar uchun arabcha matnning boshi yetarli */
internal const val PREVIEW_CHARS = 400

private val RiseEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Veb nusxadagi animate-rise: 8dp pastdan ko'tarilib paydo bo'ladi, to'rda har biri 25ms kechikib.
 * Bir marta o'ynaydi — ro'yxat aylantirilib qaytilganda takrorlanmaydi.
 */
@Composable
internal fun riseIn(index: Int = 0): Modifier {
    if (index > 24) return Modifier
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!played) {
            delay(minOf(index, 12) * 25L)
            progress.animateTo(1f, tween(350, easing = RiseEasing))
            played = true
        }
    }
    return Modifier.graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 8.dp.toPx()
    }
}

/** Veb nusxadagi animate-fade: [key] almashganda 0.25s da paydo bo'ladi */
@Composable
internal fun fadeIn(key: Any?): Modifier {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { progress.animateTo(1f, tween(250)) }
    return Modifier.graphicsLayer { alpha = progress.value }
}

/**
 * LIST_BOX qatori: uzun ro'yxat LazyColumn ichida qatorma-qator chiziladi, shuning uchun
 * umumiy hoshiya har bir qatorda bo'lib chiziladi — burchaklar va tepa / past chiziq faqat chetki qatorlarda.
 */
internal fun Modifier.listBoxRow(index: Int, count: Int, radius: Dp, line: Color): Modifier {
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) radius else 0.dp,
        topEnd = if (first) radius else 0.dp,
        bottomStart = if (last) radius else 0.dp,
        bottomEnd = if (last) radius else 0.dp,
    )
    return this
        .clip(shape)
        .drawWithContent {
            drawContent()
            val stroke = 1.dp.toPx()
            val r = radius.toPx()
            val top = if (first) stroke / 2 else -r - stroke
            val bottom = if (last) size.height - stroke / 2 else size.height + r + stroke
            drawRoundRect(
                color = line,
                topLeft = Offset(stroke / 2, top),
                size = Size(size.width - stroke, bottom - top),
                cornerRadius = CornerRadius(r),
                style = Stroke(stroke),
            )
        }
}

/**
 * To'r (grid) LazyColumn ichida: har bir qator — bitta element. Bir qatordagi kartochkalar
 * bir xil balandlikda (h-full). [cell] ga kartochka modifikatori beriladi; fill — balandlikni to'ldirish.
 */
internal fun <T> LazyListScope.gridItems(
    list: List<T>,
    columns: Int,
    gap: Dp,
    cell: @Composable (index: Int, item: T, modifier: Modifier, fill: Boolean) -> Unit,
) {
    val rows = (list.size + columns - 1) / columns
    items(rows, contentType = { "grid-row" }) { row ->
        val from = row * columns
        val fill = columns > 1
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = if (row < rows - 1) gap else 0.dp)
                .then(if (fill) Modifier.height(IntrinsicSize.Min) else Modifier),
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (column in 0 until columns) {
                val index = from + column
                if (index < list.size) {
                    Box(Modifier.weight(1f).then(if (fill) Modifier.fillMaxHeight() else Modifier)) {
                        cell(
                            index,
                            list[index],
                            Modifier.fillMaxWidth().then(if (fill) Modifier.fillMaxHeight() else Modifier),
                            fill,
                        )
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
