package com.example.kotlin_holy.ui.feature.juz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AudioDownloadCard
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.MushafPageView
import com.example.kotlin_holy.ui.components.PageReadMark
import com.example.kotlin_holy.ui.components.PageTranslation
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.SegmentedControl
import com.example.kotlin_holy.ui.theme.HolyTheme

private val TEXT_MODES = listOf(
    "both" to "Ikkalasi",
    "arabic" to "Arabcha",
    "translation" to "Tarjima",
)

@Composable
fun JuzReadScreen(
    viewModel: JuzReadViewModel = hiltViewModel(),
    audioViewModel: ReaderAudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val juz = state.juz

    /* Oyat raqami bosilganda shu oyat yonida boshqaruv ochiladi */
    var menuVerse by remember { mutableStateOf<String?>(null) }

    /*
     * Juz o'nlab surani qamraydi, audio esa sura-ba-sura yuklanadi. Shuning
     * uchun yuklash kartasi butun juz uchun emas, ochiq turgan betdagi suralar
     * uchun chiqadi — betdan betga o'tilganda o'zi almashadi.
     */
    val pageSurahs = remember(state.content) {
        state.content?.ayahs.orEmpty()
            .map { it.surahNumber to it.surahNameUz }
            .distinctBy { it.first }
    }

    val expectedSizes = remember { mutableStateMapOf<Int, Long>() }
    LaunchedEffect(pageSurahs, audio.downloads.keys) {
        pageSurahs.forEach { (surahNumber, _) ->
            if (!audio.isReady(surahNumber) && expectedSizes[surahNumber] == null) {
                audioViewModel.expectedSize(surahNumber)?.let { expectedSizes[surahNumber] = it }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        Text(
            text = "${juz?.number ?: "-"}-juz · ${state.pageNumber}-sahifa",
            color = colors.inkFaint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
        )

        SegmentedControl(
            options = TEXT_MODES,
            value = state.textMode,
            onChange = viewModel::setTextMode,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        pageSurahs.forEach { (surahNumber, surahName) ->
            AudioDownloadCard(
                state = audio.downloadOf(surahNumber),
                expectedBytes = expectedSizes[surahNumber],
                title = "${surahName ?: "$surahNumber-sura"} qiroati",
                onDownload = { audioViewModel.download(surahNumber) },
                onCancel = { audioViewModel.cancelDownload(surahNumber) },
                onDelete = { audioViewModel.deleteAudio(surahNumber) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (state.loading) {
            LoadingRows(rows = 6)
        } else {
            HolyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 12.dp)) {
                    val mushaf = state.mushaf
                    if (state.showArabic && mushaf != null) {
                        MushafPageView(
                            page = mushaf,
                            scale = state.arabicScale,
                            highlightedVerse = state.highlightedVerse,
                            playingVerse = audio.playingVerse,
                            playingWord = audio.playingWord,
                            menuVerse = menuVerse,
                            isPlaying = audio.isPlayingAyah,
                            audioReady = audio.isReady(
                                menuVerse?.substringBefore(':')?.toIntOrNull()
                                    ?: pageSurahs.firstOrNull()?.first ?: 0,
                            ),
                            onVerseClick = viewModel::onVerseClick,
                            onWordClick = { surahNumber, ayahNumber, wordNumber ->
                                if (audio.isReady(surahNumber)) {
                                    audioViewModel.playWord(surahNumber, ayahNumber, wordNumber)
                                }
                            },
                            onMenuOpen = { verseKey -> menuVerse = verseKey },
                            onMenuDismiss = { menuVerse = null },
                            onPlay = audioViewModel::playAyah,
                            onPlayFrom = audioViewModel::playFrom,
                            onPause = audioViewModel::pause,
                            onStop = {
                                audioViewModel.stop()
                                menuVerse = null
                            },
                        )
                    }
                    if (state.showArabic && state.showTranslation) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp)
                                .height(1.dp)
                                .background(colors.line),
                        )
                    }
                    val content = state.content
                    if (state.showTranslation && content != null) {
                        PageTranslation(ayahs = content.ayahs, scale = state.translationScale)
                    }
                }
            }

            PageReadMark(readAt = state.readAt, onToggle = viewModel::toggleRead)

            ReaderPager(
                label = "${state.pageNumber} / ${juz?.toPage ?: 604}",
                onPrev = if (juz != null && state.pageNumber > juz.fromPage) {
                    ({ viewModel.move(-1) })
                } else {
                    null
                },
                onNext = if (juz != null && state.pageNumber < juz.toPage) {
                    ({ viewModel.move(1) })
                } else {
                    null
                },
                prevLabel = "Oldingi sahifa",
                nextLabel = "Keyingi sahifa",
            )
        }
    }
}
