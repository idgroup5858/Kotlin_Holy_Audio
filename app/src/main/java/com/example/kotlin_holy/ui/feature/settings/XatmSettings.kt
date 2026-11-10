package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.repository.XatmRepository
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.components.formatReadAt
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.percentLabel
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.JakartaSans
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TOTAL_PAGES = JuzCatalog.TOTAL_PAGES
private val RED_500 = Color(0xFFEF4444)

data class XatmSettingsState(
    /** Bet raqami → belgilangan vaqt */
    val pages: Map<Int, Long> = emptyMap(),
    val startedAt: Long? = null,
) {
    val readCount: Int get() = pages.size

    /** Oraliq bilan belgilanganda vaqtlar bir xil bo'ladi — u holda eng katta bet oxirgisi */
    val last: Map.Entry<Int, Long>? get() = pages.entries.maxWithOrNull(compareBy({ it.value }, { it.key }))
}

@HiltViewModel
class XatmSettingsViewModel @Inject constructor(
    private val xatmRepository: XatmRepository,
) : ViewModel() {

    val state: StateFlow<XatmSettingsState> =
        combine(xatmRepository.readPages, xatmRepository.startedAt) { pages, started ->
            XatmSettingsState(pages, started)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), XatmSettingsState())

    fun setRange(from: Int, to: Int, read: Boolean) {
        viewModelScope.launch { xatmRepository.setRange(from, to, read) }
    }

    fun reset() {
        viewModelScope.launch { xatmRepository.reset() }
    }
}

/** Sozlamalar → Xatm (veb nusxadagi XatmSettings.jsx) */
internal fun LazyListScope.xatmSettings() {
    item(key = "xatm-status") { Box(Modifier.padding(bottom = 20.dp)) { XatmStatus() } }
    item(key = "xatm-range") { Box(Modifier.padding(bottom = 20.dp)) { RangeMark() } }
    item(key = "xatm-reset") { Box(Modifier.padding(bottom = 20.dp)) { XatmReset() } }
}

/* ---------------- Xatm holati ---------------- */

@Composable
private fun XatmStatus() {
    val viewModel: XatmSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val nav = LocalNav.current
    val readCount = state.readCount
    val percent = Math.round(readCount.toDouble() / TOTAL_PAGES * 1000) / 10.0
    val last = state.last

    SettingsCard {
        SectionTitle("Xatm holati")

        Row(Modifier.fillMaxWidth().padding(top = 20.dp)) {
            Text(
                "$readCount",
                color = colors.read,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                style = tabular(),
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "/ $TOTAL_PAGES sahifa oʻqildi",
                color = colors.inkFaint,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp).alignByBaseline(),
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${percentLabel(percent)}%",
                color = colors.inkSoft,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                style = tabular(),
                modifier = Modifier.alignByBaseline(),
            )
        }

        val ratio by animateFloatAsState(
            (readCount.toFloat() / TOTAL_PAGES).coerceIn(0f, 1f),
            tween(300),
            label = "xatm",
        )
        Box(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(colors.surface2),
        ) {
            Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(CircleShape).background(colors.read))
        }

        val strong = SpanStyle(color = colors.ink, fontWeight = FontWeight.SemiBold)
        val text = buildAnnotatedString {
            if (last != null) {
                append("Boshlangan vaqti: ")
                withStyle(strong) { append(formatReadAt(state.startedAt ?: last.value)) }
                withStyle(SpanStyle(color = colors.line)) { append("\u2002|\u2002") }
                append("Oxirgi belgilangan: ")
                withLink(
                    LinkAnnotation.Clickable("page", TextLinkStyles(strong)) { nav.open(Routes.page(last.key)) },
                ) { append("${last.key}-sahifa") }
                append(" · ${formatReadAt(last.value)}")
            } else {
                append("Hali birorta sahifa belgilanmagan. ")
                withLink(
                    LinkAnnotation.Clickable(
                        "pages",
                        TextLinkStyles(SpanStyle(color = colors.accent, fontWeight = FontWeight.SemiBold)),
                    ) { nav.open(Routes.PAGES) },
                ) { append("Sahifalar") }
                append(" boʻlimida sahifa burchagidagi belgini bosing.")
            }
        }
        Text(text, color = colors.inkFaint, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 16.dp))
    }
}

/* ---------------- Oraliqni belgilash ---------------- */

/** Bir necha betni birdaniga belgilash: masalan, boshqa joyda o'qilgan 21–30-betlar */
@Composable
private fun RangeMark() {
    val viewModel: XatmSettingsViewModel = hiltViewModel()
    val colors = HolyTheme.colors
    var from by rememberSaveable { mutableStateOf("") }
    var to by rememberSaveable { mutableStateOf("") }
    /* (muvaffaqiyatli, matn) */
    var message by remember { mutableStateOf<Pair<Boolean, String>?>(null) }

    fun apply(read: Boolean) {
        val a = from.toIntOrNull()
        val b = if (to.isEmpty()) a else to.toIntOrNull()
        val valid = { n: Int? -> n != null && n in 1..TOTAL_PAGES }
        if (!valid(a) || !valid(b)) {
            message = false to "Sahifa raqami 1 dan $TOTAL_PAGES gacha boʻlishi kerak."
            return
        }
        val start = minOf(a!!, b!!)
        val end = maxOf(a, b)
        viewModel.setRange(start, end, read)
        val label = if (start == end) "$start-sahifa" else "$start–$end-sahifalar"
        val count = end - start + 1
        message = true to if (read) {
            "$label oʻqildi deb belgilandi ($count ta)."
        } else {
            "$label belgisi olib tashlandi ($count ta)."
        }
    }

    SettingsCard {
        SectionTitle("Oraliqni belgilash")
        HintText(
            "Boshqa joyda oʻqigan sahifalaringizni birma-bir bosib oʻtirmasdan, oraliq bilan belgilang. " +
                "Bitta sahifa uchun faqat birinchi maydonni toʻldirish kifoya.",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 14.sp,
            relaxed = true,
        )

        FlowRow(
            modifier = Modifier.padding(top = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            PageField(from, { from = it }, "dan", "Boshlanish sahifasi", onDone = { if (from.isNotEmpty()) apply(true) })
            Text("–", color = colors.inkFaint, fontSize = 14.sp)
            PageField(to, { to = it }, "gacha", "Tugash sahifasi", onDone = { if (from.isNotEmpty()) apply(true) })
            ReadButton("Oʻqildi deb belgilash", onClick = { apply(true) }, enabled = from.isNotEmpty())
            HolyTextButton(
                "Belgini olib tashlash",
                onClick = { apply(false) },
                variant = ButtonVariant.Ghost,
                size = ButtonSize.Sm,
                enabled = from.isNotEmpty(),
            )
        }

        message?.let { (ok, text) ->
            Text(
                text,
                color = if (ok) colors.read else RED_500,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

/** <Input type="number" className="w-24 text-center"> — faqat raqam */
@Composable
private fun PageField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    description: String,
    onDone: () -> Unit,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val style = TextStyle(fontSize = 14.sp, fontFamily = JakartaSans, color = colors.ink, textAlign = TextAlign.Center)

    BasicTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(3)) },
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(colors.accent),
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .width(96.dp)
            .height(32.dp)
            .then(if (focused) Modifier.border(4.dp, colors.accent.copy(alpha = 0.15f), shape) else Modifier)
            .clip(shape)
            .background(colors.surface2)
            .border(1.dp, if (focused) colors.accent else colors.line, shape)
            .semantics { contentDescription = description },
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(placeholder, style = style.copy(color = colors.inkFaint), modifier = Modifier.fillMaxWidth())
                }
                inner()
            }
        },
    )
}

/** Yashil (emerald-500) tugma — «O'qildi deb belgilash» */
@Composable
private fun ReadButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    val shape = HolyTheme.shape
    val color = HolyTheme.colors.read
    Row(
        Modifier
            .alpha(if (enabled) 1f else 0.5f)
            .height(30.dp)
            .clip(shape)
            .background(color)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/* ---------------- Belgilarni tozalash ---------------- */

@Composable
private fun XatmReset() {
    val viewModel: XatmSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirming by remember { mutableStateOf(false) }

    SettingsCard {
        SectionTitle("Belgilarni tozalash")
        HintText(
            "Barcha «oʻqildi» belgilari va ularning sanalari oʻchiriladi. Yangi xatmni boshlash uchun " +
                "ishlating. Belgilar faqat shu qurilmada saqlanadi.",
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 14.sp,
            relaxed = true,
        )
        ConfirmActions(
            confirming = confirming,
            question = "${state.readCount} ta belgi oʻchirilsinmi?",
            triggerLabel = "Barchasini tozalash",
            confirmLabel = "Ha, tozalash",
            onTrigger = { confirming = true },
            onConfirm = {
                viewModel.reset()
                confirming = false
            },
            onCancel = { confirming = false },
            modifier = Modifier.padding(top = 20.dp),
            triggerEnabled = state.readCount > 0,
        )
    }
}
