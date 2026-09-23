package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.data.audio.ReciterCatalog
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyTextField
import com.example.kotlin_holy.ui.components.ScreenHeader
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.components.SegmentedControl
import com.example.kotlin_holy.ui.components.formatReadAt
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ThemeMode

private val THEMES = listOf(
    "SYSTEM" to "Tizim",
    "LIGHT" to "Yorugʻ",
    "DARK" to "Qorongʻi",
)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader(title = "Sozlamalar") }

        /* ---- Ko'rinish ---- */
        item {
            HolyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle("Koʻrinish")
                    Text(
                        "Mavzu",
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                    )
                    SegmentedControl(
                        options = THEMES,
                        value = state.settings.themeMode.name,
                        onChange = { viewModel.setTheme(ThemeMode.valueOf(it)) },
                    )

                    ScaleSlider(
                        label = "Arabcha matn oʻlchami",
                        value = state.settings.arabicScale,
                        onChange = viewModel::setArabicScale,
                    )
                    ScaleSlider(
                        label = "Tarjima oʻlchami",
                        value = state.settings.translationScale,
                        onChange = viewModel::setTranslationScale,
                    )
                }
            }
        }

        /* ---- Qori tanlash ---- */
        item {
            HolyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle("Qiroat")
                    Text(
                        "Sura audiosi tanlangan qori ovozida yuklanadi. Qorini " +
                            "almashtirsangiz, avval yuklanganlari oʻz joyida qoladi.",
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 10.dp),
                    )
                    ReciterCatalog.all.forEach { reciter ->
                        ReciterRow(
                            nameUz = reciter.nameUz,
                            style = reciter.style,
                            selected = reciter.id == state.settings.reciterId,
                            onClick = { viewModel.setReciter(reciter.id) },
                        )
                    }
                }
            }
        }

        /* ---- Yuklangan qiroatlar ---- */
        item {
            RecitationStorageCard(
                storage = state.audio,
                reciterLabel = state.reciter.label,
                onDelete = viewModel::deleteRecitation,
                onDeleteAll = viewModel::deleteAllRecitations,
            )
        }

        /* ---- Xatm holati ---- */
        item {
            HolyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle("Xatm holati")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Text(
                            state.readCount.toString(),
                            color = colors.read,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(
                            " / ${JuzCatalog.TOTAL_PAGES} sahifa oʻqildi",
                            color = colors.inkFaint,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f).padding(start = 4.dp, bottom = 4.dp),
                        )
                        Text(
                            "${(state.percent * 1000).toInt() / 10f}%",
                            color = colors.inkSoft,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(colors.surface2),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(state.percent.coerceIn(0f, 1f))
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(colors.read),
                        )
                    }

                    val last = state.lastMarked
                    Text(
                        text = if (last != null) {
                            "Oxirgi belgilangan: ${last.first}-sahifa · ${formatReadAt(last.second)}"
                        } else {
                            "Hali birorta sahifa belgilanmagan."
                        },
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }

        /* ---- Oraliqni belgilash ---- */
        item { RangeMarkCard(onApply = viewModel::markRange) }

        /* ---- Tozalash ---- */
        item { ResetCard(readCount = state.readCount, onReset = viewModel::resetXatm) }

        /* ---- Ma'lumot ---- */
        item {
            HolyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    SectionTitle("Maʼlumot")
                    val meta = state.meta
                    Text(
                        text = if (meta != null) {
                            "${meta.surahs} sura · ${meta.ayahs} oyat · ${meta.pages} sahifa\n" +
                                "${meta.mushafPages} mushaf beti · ${meta.words} qiyin soʻz"
                        } else {
                            "Maʼlumot fayllari oʻqilmadi."
                        },
                        color = colors.inkSoft,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Text(
                        "Barcha matn ilova ichidagi fayllardan oʻqiladi — internet kerak emas. " +
                            "Internet faqat qiroatni yuklab olishda kerak boʻladi; yuklangach " +
                            "u ham qurilmadan oʻqiladi.",
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScaleSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val colors = HolyTheme.colors
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = colors.inkFaint, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                "${(value * 100).toInt()}%",
                color = colors.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0.7f..1.6f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = colors.accent,
                activeTrackColor = colors.accent,
                inactiveTrackColor = colors.surface2,
            ),
        )
    }
}

@Composable
private fun RangeMarkCard(onApply: (Int, Int, Boolean) -> Unit) {
    val colors = HolyTheme.colors
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    fun apply(read: Boolean) {
        val a = from.toIntOrNull()
        val b = to.toIntOrNull() ?: a
        if (a == null || b == null || a !in 1..JuzCatalog.TOTAL_PAGES || b !in 1..JuzCatalog.TOTAL_PAGES) {
            message = "Sahifa raqami 1 dan ${JuzCatalog.TOTAL_PAGES} gacha boʻlishi kerak."
            return
        }
        val start = minOf(a, b)
        val end = maxOf(a, b)
        onApply(start, end, read)
        val label = if (start == end) "$start-sahifa" else "$start–$end-sahifalar"
        message = if (read) {
            "$label oʻqildi deb belgilandi (${end - start + 1} ta)."
        } else {
            "$label belgisi olib tashlandi (${end - start + 1} ta)."
        }
    }

    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            SectionTitle("Oraliqni belgilash")
            Text(
                "Boshqa joyda oʻqigan sahifalarni birma-bir bosmasdan belgilang.",
                color = colors.inkFaint,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NumberField(value = from, onChange = { from = it }, placeholder = "dan")
                Text("–", color = colors.inkFaint)
                NumberField(value = to, onChange = { to = it }, placeholder = "gacha")
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Oʻqildi deb belgilash",
                    color = androidx.compose.ui.graphics.Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.read)
                        .clickable { apply(true) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(
                    "Belgini olib tashlash",
                    color = colors.inkSoft,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable { apply(false) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }

            if (message != null) {
                Text(
                    message!!,
                    color = colors.read,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, placeholder: String) {
    HolyTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter { it.isDigit() }.take(3)) },
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.width(90.dp),
    )
}

@Composable
private fun ResetCard(readCount: Int, onReset: () -> Unit) {
    val colors = HolyTheme.colors
    var confirming by remember { mutableStateOf(false) }

    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            SectionTitle("Belgilarni tozalash")
            Text(
                "Barcha «oʻqildi» belgilari va sanalari oʻchiriladi.",
                color = colors.inkFaint,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            if (confirming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "$readCount ta belgi oʻchirilsinmi?",
                        color = colors.ink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Ha, tozalash",
                        color = colors.danger,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                onReset()
                                confirming = false
                            }
                            .padding(6.dp),
                    )
                    Text(
                        "Bekor qilish",
                        color = colors.inkSoft,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable { confirming = false }.padding(6.dp),
                    )
                }
            } else {
                Text(
                    "Barchasini tozalash",
                    color = if (readCount > 0) colors.danger else colors.inkFaint,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable(enabled = readCount > 0) { confirming = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}
