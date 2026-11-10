package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.REMINDER_TYPES
import com.example.kotlin_holy.domain.model.Reminder
import com.example.kotlin_holy.domain.model.ReminderVerse
import com.example.kotlin_holy.domain.model.reminderTypeLabel
import com.example.kotlin_holy.domain.repository.RemindersRepository
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.EmptyState
import com.example.kotlin_holy.ui.components.FormField
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyInput
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.components.Segmented
import com.example.kotlin_holy.ui.components.formatReadAt
import com.example.kotlin_holy.ui.theme.AmiriQuran
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.JakartaSans
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val TITLE_PLACEHOLDERS = mapOf(
    "ayah" to "Masalan: Baqara surasi, 286-oyat",
    "hadith" to "Masalan: Sahih Buxoriy, 1-hadis",
    "hikmat" to "Masalan: Sabr haqida",
)

/* Arabcha harflar — eslatma satri arabcha shriftda chiqishi uchun */
private val ARABIC_LETTER = Regex("[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]")
private val LATIN_LETTER = Regex("[A-Za-z]")

/* Mushaf glifi o'zi baland — eslatmada satrlar bir-biridan uzoqlashib ketmasligi uchun */
private const val REMINDER_VERSE_LINE_HEIGHT = 1.8f

/** Eslatmalar sahifasi qatorga tushadigan ma'lumot: ro'yxat (null — yuklanmoqda) va forma ochiqmi */
@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val repository: RemindersRepository,
) : ViewModel() {

    val reminders: StateFlow<List<Reminder>?> =
        repository.reminders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /*
     * Ochiq forma raqami (null — yopiq). Har ochilishda yangi raqam: ro'yxat qatori shu raqam
     * bilan belgilanadi, shuning uchun forma har safar bo'sh ochiladi (avvalgi matn qaytmaydi).
     */
    private val _form = MutableStateFlow<Int?>(null)
    val form: StateFlow<Int?> = _form.asStateFlow()
    private var nextForm = 0

    fun openForm() {
        if (_form.value == null) _form.value = nextForm++
    }

    fun closeForm() {
        _form.value = null
    }

    fun add(type: String, title: String, text: String) {
        viewModelScope.launch { repository.add(type, title, text) }
        _form.value = null
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }
}

/** Eslatmalar tabining holati — ro'yxat uzun bo'lishi mumkin, har bir eslatma alohida qator */
@Stable
internal class ReminderTabState(
    val viewModel: ReminderSettingsViewModel,
    val reminders: List<Reminder>?,
    /** Ochiq forma raqami; null — forma yopiq */
    val form: Int?,
) {
    val adding: Boolean get() = form != null
}

@Composable
internal fun rememberReminderTabState(): ReminderTabState {
    val viewModel: ReminderSettingsViewModel = hiltViewModel()
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    return ReminderTabState(viewModel, reminders, form)
}

/* max-w-3xl */
private val MAX_WIDTH = 768.dp

@Composable
private fun Column3xl(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Box(Modifier.widthIn(max = MAX_WIDTH)) { content() }
    }
}

/** Sozlamalar → Eslatmalar (veb nusxadagi ReminderSettings.jsx) */
internal fun LazyListScope.reminderSettings(state: ReminderTabState) {
    val viewModel = state.viewModel
    item(key = "reminders-header") {
        Column3xl {
            ReminderHeader(
                count = state.reminders?.size ?: 0,
                adding = state.adding,
                onAdd = viewModel::openForm,
            )
        }
    }
    state.form?.let { form ->
        item(key = "reminders-form-$form") {
            Column3xl {
                ReminderForm(
                    onSave = viewModel::add,
                    onCancel = viewModel::closeForm,
                )
            }
        }
    }
    val reminders = state.reminders ?: return
    if (reminders.isEmpty() && !state.adding) {
        item(key = "reminders-empty") {
            Column3xl {
                EmptyState(
                    title = "Hali eslatma yoʻq",
                    hint = "Yoqqan oyat, hadis yoki hikmatingizni nusxalab, «Eslatma qoʻshish» orqali saqlang.",
                )
            }
        }
    }
    items(reminders, key = { "reminder-${it.id}" }) { reminder ->
        Column3xl { ReminderItem(reminder, onDelete = { viewModel.delete(reminder.id) }) }
    }
}

@Composable
private fun ReminderHeader(count: Int, adding: Boolean, onAdd: () -> Unit) {
    SettingsCard {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                SectionTitle("Eslatmalar", trailing = { HolyBadge("$count") })
                HintText(
                    "Dastur har ochilganda ulardan biri bosh sahifadagi «Bugungi eslatma» tabida " +
                        "tasodifiy chiqadi. Eslatmalar shu qurilmada saqlanadi.",
                    modifier = Modifier.padding(top = 4.dp),
                    relaxed = true,
                )
            }
            if (!adding) {
                HolyTextButton("+ Eslatma qoʻshish", onClick = onAdd, variant = ButtonVariant.Primary)
            }
        }
    }
}

@Composable
private fun ReminderForm(onSave: (type: String, title: String, text: String) -> Unit, onCancel: () -> Unit) {
    val colors = HolyTheme.colors
    var type by rememberSaveable { mutableStateOf("ayah") }
    var title by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    val canSave = title.isNotBlank() && text.isNotBlank()

    /* autoFocus — forma ochilganda bir marta */
    val focus = remember { FocusRequester() }
    var focused by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!focused) {
            focused = true
            runCatching { focus.requestFocus() }
        }
    }

    SettingsCard {
        SectionTitle("Yangi eslatma")
        Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Turi", color = colors.inkSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Segmented(options = REMINDER_TYPES, value = type, onChange = { type = it })
            }

            FormField(label = "Sarlavha") {
                HolyInput(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    placeholder = TITLE_PLACEHOLDERS[type],
                    minHeight = 32.dp,
                )
            }

            FormField(label = "Matn", hint = "Arabcha satrlar avtomatik ravishda arabcha shriftda koʻrsatiladi.") {
                HolyInput(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Oyat, hadis yoki hikmat matnini shu yerga yozing yoki joylang",
                    singleLine = false,
                    minHeight = 158.dp,
                    /* dir="auto": satr yo'nalishi matnning o'zidan aniqlanadi */
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 22.75.sp,
                        fontFamily = JakartaSans,
                        textDirection = TextDirection.Content,
                    ),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HolyTextButton(
                    "Saqlash",
                    onClick = { if (canSave) onSave(type, title, text) },
                    variant = ButtonVariant.Primary,
                    enabled = canSave,
                )
                HolyTextButton("Bekor qilish", onClick = onCancel, variant = ButtonVariant.Ghost)
            }
        }
    }
}

@Composable
private fun ReminderItem(reminder: Reminder, onDelete: () -> Unit) {
    val colors = HolyTheme.colors
    var confirming by remember { mutableStateOf(false) }

    SettingsCard(padding = 20.dp) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HolyBadge(
                        reminderTypeLabel(reminder.type),
                        tone = if (reminder.type == "ayah") BadgeTone.Accent else BadgeTone.Neutral,
                    )
                    Text(
                        formatReadAt(reminder.createdAt),
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        style = tabular(),
                    )
                }
                if (reminder.title.isNotBlank()) {
                    Text(
                        reminder.title,
                        color = colors.ink,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            ConfirmActions(
                confirming = confirming,
                question = "Oʻchirilsinmi?",
                triggerLabel = "Oʻchirish",
                confirmLabel = "Ha, oʻchirish",
                onTrigger = { confirming = true },
                onConfirm = {
                    confirming = false
                    onDelete()
                },
                onCancel = { confirming = false },
                questionSize = 13.sp,
            )
        }

        ReminderText(reminder.text, reminder.verses, Modifier.padding(top = 12.dp))
    }
}

/* Arabcha harflari ko'p satr arabcha shriftda, o'ngdan chapga chiqadi */
private fun isArabicLine(line: String): Boolean =
    ARABIC_LETTER.findAll(line).count() > LATIN_LETTER.findAll(line).count()

/**
 * Eslatma matni (compact): har bir satr alohida — arabcha oyat va tarjimasi aralash bo'lishi mumkin.
 * `verses` bo'lsa, oyatlar sahifalardagi mushaf shriftida chiqadi.
 */
@Composable
private fun ReminderText(text: String, verses: List<ReminderVerse>, modifier: Modifier = Modifier) {
    val colors = HolyTheme.colors
    val lines = remember(text) {
        text.split(Regex("\r?\n")).map { it.trim() }.filter { it.isNotEmpty() }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        verses.forEach { verse ->
            MushafAyahText(
                surahNumber = verse.surah,
                ayahNumber = verse.ayah,
                pageNumber = verse.page,
                lineHeight = REMINDER_VERSE_LINE_HEIGHT,
            )
        }
        lines.forEach { line ->
            if (isArabicLine(line)) {
                Text(
                    line,
                    style = TextStyle(
                        fontFamily = AmiriQuran,
                        fontSize = 24.sp,
                        lineHeight = (24 * 2.3f).sp,
                        textAlign = TextAlign.Right,
                        textDirection = TextDirection.Rtl,
                        color = colors.ink,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    line,
                    color = colors.inkSoft,
                    fontSize = 14.sp,
                    lineHeight = 22.75.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
