package com.example.kotlin_holy.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.audio.QuranPlayer
import com.example.kotlin_holy.data.audio.ReciterCatalog
import com.example.kotlin_holy.domain.model.AudioDownload
import com.example.kotlin_holy.domain.model.JuzCatalog
import com.example.kotlin_holy.domain.model.Reciter
import com.example.kotlin_holy.domain.repository.AudioRepository
import com.example.kotlin_holy.domain.repository.DEFAULT_RECITER_ID
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.ListBox
import com.example.kotlin_holy.ui.components.rowBackground
import com.example.kotlin_holy.ui.theme.HolyTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val RED_500 = Color(0xFFEF4444)

/** Qurilmadagi bitta suraning qiroati: hajmi yoki (yuklanayotgan bo'lsa) foizi */
data class StoredRecitation(
    val surahNumber: Int,
    val nameUz: String,
    val bytes: Long?,
    val progress: Int? = null,
)

data class AudioSettingsState(
    val loaded: Boolean = false,
    val reciterId: Int = DEFAULT_RECITER_ID,
    /** Barcha qorilar bo'yicha egallangan joy */
    val totalBytes: Long = 0,
    /** Tanlangan qori uchun yuklangan (va yuklanayotgan) suralar */
    val recitations: List<StoredRecitation> = emptyList(),
) {
    val reciter: Reciter get() = ReciterCatalog.byId(reciterId)
    val readyCount: Int get() = recitations.count { it.progress == null }
}

/**
 * Qiroat sozlamalari — faqat Android nusxasida: qori tanlash, egallangan joy,
 * suralarni alohida yoki hammasini birdan o'chirish.
 */
@HiltViewModel
class AudioSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val audioRepository: AudioRepository,
    private val player: QuranPlayer,
    quranRepository: QuranRepository,
) : ViewModel() {

    private val surahNames = MutableStateFlow<Map<Int, String>>(emptyMap())

    /** Egallangan joy va yuklangan suralar fayllardan sanaladi — shuning uchun alohida */
    private val storage = MutableStateFlow<Pair<Long, List<Int>>?>(null)

    val state: StateFlow<AudioSettingsState> = combine(
        settingsRepository.settings.map { it.reciterId }.distinctUntilChanged(),
        audioRepository.downloads,
        storage,
        surahNames,
    ) { reciterId, downloads, stored, names ->
        val downloaded = stored?.second.orEmpty()
        val numbers = (downloaded + downloads.filterValues { it is AudioDownload.Ready || it is AudioDownload.InProgress }.keys)
            .distinct()
            .sorted()
        AudioSettingsState(
            loaded = stored != null,
            reciterId = reciterId,
            totalBytes = stored?.first ?: 0L,
            recitations = numbers.mapNotNull { surah ->
                val name = names[surah] ?: "$surah-sura"
                when (val download = downloads[surah]) {
                    is AudioDownload.InProgress -> StoredRecitation(surah, name, null, download.percent)
                    is AudioDownload.Ready -> StoredRecitation(surah, name, download.bytes)
                    else -> if (surah in downloaded) StoredRecitation(surah, name, null) else null
                }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioSettingsState())

    init {
        viewModelScope.launch {
            surahNames.value = runCatching {
                quranRepository.surahs().associate { it.number to it.nameUz }
            }.getOrDefault(emptyMap())
        }
        /* Sura yuklab bo'linsa yoki o'chirilsa, egallangan joy qayta sanaladi (foiz o'zgarganda emas) */
        viewModelScope.launch {
            audioRepository.downloads
                .map { downloads -> downloads.filterValues { it is AudioDownload.Ready }.keys }
                .distinctUntilChanged()
                .collect { refreshStorage() }
        }
    }

    private suspend fun refreshStorage() {
        storage.value = runCatching {
            audioRepository.storageBytes() to audioRepository.downloadedSurahs()
        }.getOrDefault(0L to emptyList())
    }

    /**
     * Qori almashtirilganda ijro to'xtatiladi: eski qori fayli ochiq turgan bo'lsa,
     * yangi tanlov bilan aralashib ketmasligi kerak. Eski yuklamalar o'chirilmaydi —
     * ular o'z papkasida qoladi va qori qaytarilsa yana ishlaydi.
     */
    fun setReciter(id: Int) {
        if (id == state.value.reciterId) return
        viewModelScope.launch {
            player.stop()
            settingsRepository.setReciter(id)
            refreshStorage()
        }
    }

    fun deleteRecitation(surahNumber: Int) {
        viewModelScope.launch {
            if (player.state.value.surahNumber == surahNumber) player.stop()
            audioRepository.delete(surahNumber)
            refreshStorage()
        }
    }

    /** Ijro avval to'xtatiladi — ochiq fayl o'chirilib ketmasin */
    fun deleteAll() {
        viewModelScope.launch {
            player.stop()
            audioRepository.deleteAll()
            refreshStorage()
        }
    }
}

/** Sozlamalar → Qiroat (faqat Android nusxasida) */
internal fun LazyListScope.audioSettings() {
    item(key = "audio-reciter") { Box(Modifier.padding(bottom = 20.dp)) { ReciterSection() } }
    item(key = "audio-storage") { Box(Modifier.padding(bottom = 20.dp)) { StorageSection() } }
    item(key = "audio-note") {
        HintText(
            "Audio butun Qurʼon uchun emas, faqat ochilgan sura uchun yuklanadi: bir sura — bitta fayl " +
                "va uning vaqt jadvali. Yuklangani qurilmada qoladi va internetsiz ishlaydi; internet faqat " +
                "qiroatni yuklab olishda kerak boʻladi.",
            modifier = Modifier.padding(bottom = 20.dp),
            relaxed = true,
        )
    }
}

@Composable
private fun ReciterSection() {
    val viewModel: AudioSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val active = state.reciter

    SettingsSection(
        title = "Qori",
        hint = "Sura audiosi tanlangan qori ovozida yuklanadi. Qorini almashtirsangiz, avval " +
            "yuklanganlari oʻz joyida qoladi.",
        aside = { AsideLabel(active.nameUz) },
    ) {
        GridRows(
            ReciterCatalog.all,
            columns = columnsFor(1, sm = 2, lg = 3),
            horizontalGap = 12.dp,
            verticalGap = 12.dp,
        ) { reciter, modifier ->
            val selected = reciter.id == active.id
            OptionTile(
                selected = selected,
                onClick = { viewModel.setReciter(reciter.id) },
                modifier = modifier,
                contentPadding = PaddingValues(14.dp),
                label = reciter.label,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selected) colors.accent else colors.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            HolyIcons.Headphones,
                            contentDescription = null,
                            tint = if (selected) Color.White else colors.inkSoft,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column {
                        Text(reciter.nameUz, color = colors.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(reciter.style, color = colors.inkFaint, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageSection() {
    val viewModel: AudioSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    var confirming by remember { mutableStateOf(false) }
    val total = state.totalBytes

    SettingsSection(
        title = "Yuklangan qiroatlar",
        hint = "Qiroat sura ekranidan yuklanadi — ${state.reciter.label}",
        aside = if (total > 0) {
            { HolyBadge(formatSize(total), tone = BadgeTone.Accent) }
        } else {
            null
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StatRow("Shu qorida yuklangan suralar", "${state.readyCount} / ${JuzCatalog.TOTAL_SURAHS}")
            StatRow(
                "Egallangan joy (barcha qorilar)",
                if (total > 0) formatSize(total) else "—",
                strong = total > 0,
            )
        }

        if (state.loaded && state.recitations.isEmpty()) {
            HintText("Hali birorta sura yuklanmagan.", modifier = Modifier.padding(top = 16.dp))
        }

        if (state.recitations.isNotEmpty()) {
            ListBox(Modifier.padding(top = 16.dp)) {
                state.recitations.forEachIndexed { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(rowBackground(index))
                            .padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            "${item.surahNumber}. ${item.nameUz}",
                            color = colors.ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            when {
                                item.progress != null -> "yuklanmoqda… ${item.progress}%"
                                item.bytes != null -> formatSize(item.bytes)
                                else -> "—"
                            },
                            color = colors.inkFaint,
                            fontSize = 12.sp,
                            style = tabular(),
                        )
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(HolyTheme.shape)
                                .then(
                                    if (item.progress == null) {
                                        Modifier.clickable { viewModel.deleteRecitation(item.surahNumber) }
                                    } else {
                                        Modifier
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (item.progress == null) {
                                Icon(
                                    HolyIcons.Trash,
                                    contentDescription = "${item.nameUz} qiroatini oʻchirish",
                                    tint = RED_500,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        ConfirmActions(
            confirming = confirming,
            question = "Barcha qiroatlar oʻchirilsinmi?",
            triggerLabel = "Barcha qiroatlarni oʻchirish",
            confirmLabel = "Ha, oʻchirish",
            onTrigger = { confirming = true },
            onConfirm = {
                viewModel.deleteAll()
                confirming = false
            },
            onCancel = { confirming = false },
            modifier = Modifier.padding(top = 20.dp),
            triggerEnabled = total > 0,
        )
    }
}
