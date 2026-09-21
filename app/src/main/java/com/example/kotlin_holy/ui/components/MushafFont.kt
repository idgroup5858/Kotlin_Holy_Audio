package com.example.kotlin_holy.ui.components

import android.content.Context
import android.graphics.Typeface
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily


/**
 * Mushafning har bir betida o'z shrifti bor (p1.ttf … p604.ttf): bet matni
 * oddiy harflar emas, o'sha bet uchun chizilgan gliflardan iborat.
 *
 * Shrift assets'dan to'g'ridan-to'g'ri (sinxron) yuklanadi. Bu muhim: satr eni
 * aynan shu shrift bilan o'lchanadi, aks holda o'lcham zaxira shrift bo'yicha
 * chiqib, satrlar betga sig'may qoladi.
 */
object MushafFontProvider {

    private val typefaces = LruCache<Int, Typeface>(24)
    private val families = LruCache<Int, FontFamily>(24)

    fun typeface(context: Context, pageNumber: Int): Typeface =
        typefaces[pageNumber] ?: Typeface
            .createFromAsset(context.assets, "fonts/hafs/p$pageNumber.ttf")
            .also { typefaces.put(pageNumber, it) }

    fun family(context: Context, pageNumber: Int): FontFamily =
        families[pageNumber] ?: FontFamily(typeface(context, pageNumber))
            .also { families.put(pageNumber, it) }
}

/** Bet shrifti: chizish uchun FontFamily va o'lchash uchun Typeface */
@Composable
fun rememberMushafFont(pageNumber: Int): Pair<FontFamily, Typeface> {
    val context = LocalContext.current
    return remember(pageNumber) {
        MushafFontProvider.family(context, pageNumber) to
            MushafFontProvider.typeface(context, pageNumber)
    }
}
