package com.example.kotlin_holy.ui.feature.pageread

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AudioDownloadCard
import com.example.kotlin_holy.ui.components.EmptyState
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
fun PageReadScreen(
    onOpenPage: (Int) -> Unit,
    viewModel: PageReadViewModel = hiltViewModel(),
    audioViewModel: ReaderAudioViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors

    /* Oyat raqami bosilganda shu oyat yonida boshqaruv ochiladi */
    var menuVerse by remember { mutableStateOf<String?>(null) }

    /* Betdagi suralar: odatda bitta, ba'zi betlarda ikkita */
    val pageSurahs = remember(state.content) {
        state.content?.ayahs.orEmpty()
            .map { it.surahNumber to it.surahNameUz }
            .distinctBy { it.first }
    }

    /* Yuklash tugmasida hajmni ko'rsatish uchun sura bo'yicha so'raladi */
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
            text = buildString {
                append("${state.number}-sahifa")
                state.content?.juz?.let { append(" · $it-juz") }
            },
            color = colors.inkFaint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 10.dp),
        )

        SegmentedControl(
            options = TEXT_MODES,
            value = state.textMode,
            onChange = viewModel::setTextMode,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        /* Qiroat sura bo'yicha yuklanadi, shuning uchun betdagi har bir sura
           uchun alohida karta chiqadi */
        pageSurahs.forEach { (surahNumber, surahName) ->
            AudioDownloadCard(
                state = audio.downloadOf(surahNumber),
                expectedBytes = expectedSizes[surahNumber],
                title = if (pageSurahs.size > 1) {
                    "${surahName ?: "$surahNumber-sura"} qiroati"
                } else {
                    null
                },
                onDownload = { audioViewModel.download(surahNumber) },
                onCancel = { audioViewModel.cancelDownload(surahNumber) },
                onDelete = { audioViewModel.deleteAudio(surahNumber) },
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        when {
            state.loading -> LoadingRows(rows = 6)

            state.content == null -> EmptyState(
                title = "${state.number}-sahifa topilmadi",
                hint = "Bu sahifa maʼlumot fayllarida yoʻq.",
            )

            else -> {
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
                                /* Har bir oyat o'z surasining qiroatiga bog'liq */
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

                        if (state.showTranslation) {
                            PageTranslation(
                                ayahs = state.content!!.ayahs,
                                scale = state.translationScale,
                            )
                        }
                    }
                }

                PageReadMark(readAt = state.readAt, onToggle = viewModel::toggleRead)

                ReaderPager(
                    label = "${state.number} / 604",
                    onPrev = if (state.number > 1) ({ onOpenPage(state.number - 1) }) else null,
                    onNext = if (state.number < 604) ({ onOpenPage(state.number + 1) }) else null,
                    prevLabel = "Oldingi sahifa",
                    nextLabel = "Keyingi sahifa",
                )
            }
        }
    }
}
