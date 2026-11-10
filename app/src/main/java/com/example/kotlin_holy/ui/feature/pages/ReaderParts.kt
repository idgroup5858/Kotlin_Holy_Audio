package com.example.kotlin_holy.ui.feature.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.example.kotlin_holy.ui.components.AyahExtras
import com.example.kotlin_holy.ui.components.AyahRow
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.PageTranslation
import com.example.kotlin_holy.ui.components.listBoxSegment
import com.example.kotlin_holy.ui.theme.HolyTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.domain.model.MushafPage
import com.example.kotlin_holy.ui.audio.ReaderAudioState
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AudioDownloadCard
import com.example.kotlin_holy.ui.components.AyahAudio
import com.example.kotlin_holy.ui.components.MushafPageView
import com.example.kotlin_holy.ui.components.Skeleton

/** Betdagi (yoki bo'lakdagi) suralar: raqami va nomi, takrorlanmasdan */
fun surahsOf(ayahs: List<Ayah>): List<Pair<Int, String?>> =
    ayahs.map { it.surahNumber to it.surahNameUz }.distinctBy { it.first }

/**
 * Qiroat kartalari — audio sura-ba-sura yuklanadi, shuning uchun ko'rinib turgan har bir
 * sura uchun alohida karta (faqat Android nusxasida).
 */
@Composable
fun SurahAudioCards(
    surahs: List<Pair<Int, String?>>,
    audio: ReaderAudioState,
    viewModel: ReaderAudioViewModel,
    modifier: Modifier = Modifier,
) {
    if (surahs.isEmpty()) return
    val expected = remember { mutableStateMapOf<Int, Long>() }
    LaunchedEffect(surahs, audio.downloads.keys) {
        surahs.forEach { (number, _) ->
            if (!audio.isReady(number) && expected[number] == null) {
                viewModel.expectedSize(number)?.let { expected[number] = it }
            }
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        surahs.forEach { (number, name) ->
            AudioDownloadCard(
                state = audio.downloadOf(number),
                expectedBytes = expected[number],
                title = if (surahs.size > 1) "${name ?: "$number-sura"} qiroati" else null,
                onDownload = { viewModel.download(number) },
                onCancel = { viewModel.cancelDownload(number) },
                onDelete = { viewModel.deleteAudio(number) },
            )
        }
    }
}

/**
 * Mushaf beti qiroat bilan: oyat raqami bosilsa shu yerda boshqaruv (o'qish, to'xtatish,
 * davomi) chiqadi, so'z bosilsa o'sha so'z o'qiladi, ijro paytida so'zlar yonadi.
 */
@Composable
fun AudioMushafPage(
    page: MushafPage?,
    audio: ReaderAudioState,
    viewModel: ReaderAudioViewModel,
    modifier: Modifier = Modifier,
) {
    if (page == null) {
        Column(modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(8) { Skeleton(Modifier.fillMaxWidth().height(28.dp)) }
        }
        return
    }
    var menuVerse by remember(page.pageNumber) { mutableStateOf<String?>(null) }
    var highlighted by remember(page.pageNumber) { mutableStateOf<String?>(null) }
    val firstSurah = page.lines.firstOrNull()?.words?.firstOrNull()?.surahNumber ?: 0
    Box(modifier) {
        MushafPageView(
            page = page,
            highlightedVerse = highlighted,
            playingVerse = audio.playingVerse,
            playingWord = audio.playingWord,
            menuVerse = menuVerse,
            isPlaying = audio.isPlayingAyah,
            audioReady = audio.isReady(menuVerse?.substringBefore(':')?.toIntOrNull() ?: firstSurah),
            onVerseClick = { key -> highlighted = if (highlighted == key) null else key },
            onWordClick = { surah, ayah, word -> if (audio.isReady(surah)) viewModel.playWord(surah, ayah, word) },
            onMenuOpen = { menuVerse = it },
            onMenuDismiss = { menuVerse = null },
            onPlay = { surah, ayah -> viewModel.playAyah(surah, ayah) },
            onPlayFrom = { surah, ayah -> viewModel.playFrom(surah, ayah) },
            onPause = viewModel::pause,
            onStop = {
                viewModel.stop()
                menuVerse = null
            },
        )
    }
}

/** Ayahlar bet bo'yicha guruhlanadi (Madina mushafida oyat betdan betga o'tmaydi) */
data class PageGroup(val pageNumber: Int?, val ayahs: List<Ayah>)

fun groupByPage(ayahs: List<Ayah>): List<PageGroup> {
    val groups = mutableListOf<PageGroup>()
    ayahs.forEach { ayah ->
        val last = groups.lastOrNull()
        if (last != null && last.pageNumber == ayah.pageNumber) {
            groups[groups.lastIndex] = last.copy(ayahs = last.ayahs + ayah)
        } else {
            groups += PageGroup(ayah.pageNumber, listOf(ayah))
        }
    }
    return groups
}

const val AYAHS_PER_VIEW = 10

/**
 * Oyat bo'yicha rejimdagi qatorlar: veb nusxadagi <Card className="divide-y"> — bitta karta
 * ichida ajratgichli qatorlar, LazyColumn elementlari bo'lib (oyatga aylantirish uchun).
 */
fun LazyListScope.ayahRows(
    ayahs: List<Ayah>,
    textMode: String,
    mushafPages: Map<Int, MushafPage>,
    extras: AyahExtras,
    audio: AyahAudio?,
    surahLink: Boolean,
    onOpenPage: (Int) -> Unit,
    onOpenSurah: (Int) -> Unit,
) {
    items(ayahs.size, key = { "a${ayahs[it].verseKey}" }) { index ->
        val ayah = ayahs[index]
        val colors = HolyTheme.colors
        Column(
            Modifier
                .listBoxSegment(first = index == 0, last = index == ayahs.lastIndex)
                .background(colors.surface),
        ) {
            if (index > 0) Divider()
            AyahRow(
                ayah = ayah,
                textMode = textMode,
                mushafPage = ayah.pageNumber?.let { mushafPages[it] },
                extras = extras,
                onOpenPage = onOpenPage,
                onOpenSurah = onOpenSurah,
                surahLink = surahLink,
                audio = audio,
            )
        }
    }
}

/** Sahifa bo'yicha rejimdagi bet kartasi: sarlavha, mushaf, tarjima */
@Composable
fun PageGroupCard(
    caption: String,
    textMode: String,
    mushaf: MushafPage?,
    translationAyahs: List<Ayah>,
    extras: AyahExtras,
    audio: ReaderAudioState,
    audioViewModel: ReaderAudioViewModel,
) {
    val colors = HolyTheme.colors
    val showArabic = textMode != "translation"
    val showTranslation = textMode != "arabic"
    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp)) {
            Text(
                caption,
                color = colors.inkFaint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            )
            if (showArabic) AudioMushafPage(mushaf, audio, audioViewModel)
            if (showArabic && showTranslation) Divider(Modifier.padding(vertical = 28.dp))
            if (showTranslation) PageTranslation(translationAyahs, extras)
        }
    }
}

/** Oyat bo'yicha rejimdagi qatorlar uchun qiroat holati */
fun ayahAudio(audio: ReaderAudioState, viewModel: ReaderAudioViewModel) = AyahAudio(
    isReady = audio::isReady,
    playingVerse = audio.playingVerse,
    playingWord = audio.playingWord,
    isPlaying = audio.isPlayingAyah,
    onPlay = { ayah -> viewModel.playAyah(ayah.surahNumber, ayah.numberInSurah) },
    onPause = viewModel::pause,
    onStop = viewModel::stop,
    onWord = { ayah, word -> viewModel.playWord(ayah.surahNumber, ayah.numberInSurah, word) },
)
