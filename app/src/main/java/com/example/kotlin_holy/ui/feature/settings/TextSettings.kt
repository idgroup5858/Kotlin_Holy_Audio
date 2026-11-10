package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.HolyButton
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.LocalTextScales
import com.example.kotlin_holy.ui.theme.ReadingText
import kotlin.math.abs
import kotlin.math.roundToInt

/* Veb nusxadagi lib/display.js: 80% … 180%, qadam 10% */
private const val MIN_SCALE = 0.8f
private const val MAX_SCALE = 1.8f
private const val STEP = 0.1f
private const val DEFAULT_SCALE = 1f

private const val SAMPLE_ARABIC = "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ"
private const val SAMPLE_UZ = "Hamd olamlarning Robbi — Allohgadir."

private fun clampScale(value: Float): Float {
    if (value.isNaN()) return DEFAULT_SCALE
    return ((value * 10).roundToInt() / 10f).coerceIn(MIN_SCALE, MAX_SCALE)
}

private fun Float.same(other: Float) = abs(this - other) < 0.001f

/** Sozlamalar → Matn o'lchami */
internal fun LazyListScope.textSettings() {
    item(key = "text-settings") { TextSettings() }
}

@Composable
private fun TextSettings() {
    val viewModel: SettingsViewModel = hiltViewModel()
    /* Joriy o'lchamlar butun ilovaga shu orqali tarqatiladi — namunalar ham shundan */
    val scales = LocalTextScales.current
    val isDefault = scales.arabic.same(DEFAULT_SCALE) && scales.uz.same(DEFAULT_SCALE)

    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalArrangement = Arrangement.End) {
            HolyTextButton(
                "Standart holatga qaytarish",
                onClick = {
                    viewModel.setArabicScale(DEFAULT_SCALE)
                    viewModel.setTranslationScale(DEFAULT_SCALE)
                },
                enabled = !isDefault,
            )
        }

        GridRows(
            items = listOf("arabic", "uz"),
            columns = if (isLarge()) 2 else 1,
            horizontalGap = 20.dp,
            verticalGap = 20.dp,
        ) { which, modifier ->
            if (which == "arabic") {
                ScaleControl(
                    title = "Arabcha matn",
                    hint = "Oyatlar va mushaf sahifasidagi arabcha yozuv",
                    value = scales.arabic,
                    onChange = { viewModel.setArabicScale(clampScale(it)) },
                    modifier = modifier,
                ) {
                    /* Namuna — Fotiha 2-oyati, kitobdagi (1-sahifa) mushaf shriftida */
                    MushafAyahText(
                        surahNumber = 1,
                        ayahNumber = 2,
                        pageNumber = 1,
                        lineHeight = 1.8f,
                        fallback = { Text(SAMPLE_ARABIC, style = ReadingText.arabic()) },
                    )
                }
            } else {
                ScaleControl(
                    title = "Oʻzbekcha tarjima",
                    hint = "Tarjima, transkripsiya va tafsir matnlari",
                    value = scales.uz,
                    onChange = { viewModel.setTranslationScale(clampScale(it)) },
                    modifier = modifier,
                ) {
                    Column {
                        Text(SAMPLE_UZ, style = ReadingText.uz)
                        Text(
                            "Alhamdu lillahi Rabbil-ʿalamin",
                            style = ReadingText.uzSub.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }

        HintText(
            "Eslatma: arabcha matn kattalashtirilganda oʻqish ustuni ham kengayadi, shuning " +
                "uchun mushaf satrlari chetga chiqib ketmaydi. Ekran kengligi yetmay qolganda matn " +
                "oʻsishdan toʻxtaydi.",
            modifier = Modifier.padding(top = 24.dp),
            relaxed = true,
        )
    }
}

/** Bitta o'lcham: sarlavha, foiz, [−] surgich [+] va namunasi */
@Composable
private fun ScaleControl(
    title: String,
    hint: String,
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    preview: @Composable () -> Unit,
) {
    val colors = HolyTheme.colors
    val percent = (value * 100).roundToInt()
    val atDefault = value.same(DEFAULT_SCALE)

    SettingsCard(modifier) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                SectionTitle(title)
                HintText(hint, Modifier.padding(top = 4.dp))
            }
            Text(
                "$percent%",
                color = if (atDefault) colors.inkSoft else colors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(HolyTheme.shape)
                    .background(if (atDefault) colors.surface2 else colors.accent.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HolyButton(
                onClick = { onChange(value - STEP) },
                size = ButtonSize.Icon,
                enabled = value > MIN_SCALE + 0.001f,
            ) {
                Text("−", color = LocalContentColor.current, fontSize = 18.sp, lineHeight = 18.sp)
            }
            ScaleSlider(value, onChange, title, Modifier.weight(1f))
            HolyButton(
                onClick = { onChange(value + STEP) },
                size = ButtonSize.Icon,
                enabled = value < MAX_SCALE - 0.001f,
            ) {
                Text("+", color = LocalContentColor.current, fontSize = 18.sp, lineHeight = 18.sp)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .clip(HolyTheme.shape)
                .background(colors.surface2)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            preview()
        }
    }
}

/**
 * <input type="range" class="accent-accent"> — asosiy rangdagi yo'lak va dumaloq tutqich.
 * Qiymat 0.1 qadamga yaxlitlanadi; bir xil qiymat qayta yozilmaydi.
 */
@Composable
private fun ScaleSlider(value: Float, onChange: (Float) -> Unit, label: String, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val change by rememberUpdatedState(onChange)
    val current by rememberUpdatedState(value)
    /* Sudrash paytida oxirgi yuborilgan qiymat — saqlangani hali qaytmagan bo'lishi mumkin */
    val sent = remember { floatArrayOf(Float.NaN) }

    fun valueAt(x: Float, width: Float, thumb: Float): Float {
        val usable = (width - 2 * thumb).coerceAtLeast(1f)
        val fraction = ((x - thumb) / usable).coerceIn(0f, 1f)
        return clampScale(MIN_SCALE + fraction * (MAX_SCALE - MIN_SCALE))
    }

    /* Har bir sudrash/bosish boshida sent = joriy qiymat, keyin faqat yangi qadamlar yuboriladi */
    fun send(next: Float) {
        if (sent[0].isNaN() || !sent[0].same(next)) {
            sent[0] = next
            change(next)
        }
    }

    Box(
        modifier
            .height(32.dp)
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, MIN_SCALE..MAX_SCALE, steps = 9)
                setProgress { target ->
                    change(clampScale(target))
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    sent[0] = current
                    send(valueAt(position.x, size.width.toFloat(), 8.dp.toPx()))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { position ->
                        sent[0] = current
                        send(valueAt(position.x, size.width.toFloat(), 8.dp.toPx()))
                    },
                ) { pointer, _ ->
                    pointer.consume()
                    send(valueAt(pointer.position.x, size.width.toFloat(), 8.dp.toPx()))
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val thumb = 8.dp.toPx()
            val track = 4.dp.toPx()
            val start = thumb
            val end = size.width - thumb
            val cy = size.height / 2
            val fraction = ((value - MIN_SCALE) / (MAX_SCALE - MIN_SCALE)).coerceIn(0f, 1f)
            val x = start + fraction * (end - start)
            drawRoundRect(
                color = colors.inkFaint.copy(alpha = 0.35f),
                topLeft = Offset(start, cy - track / 2),
                size = Size(end - start, track),
                cornerRadius = CornerRadius(track / 2),
            )
            drawRoundRect(
                color = colors.accent,
                topLeft = Offset(start, cy - track / 2),
                size = Size(x - start, track),
                cornerRadius = CornerRadius(track / 2),
            )
            drawCircle(colors.accent, thumb, Offset(x, cy))
        }
    }
}
