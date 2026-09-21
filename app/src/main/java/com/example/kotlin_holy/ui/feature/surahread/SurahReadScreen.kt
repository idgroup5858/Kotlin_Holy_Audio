package com.example.kotlin_holy.ui.feature.surahread

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.Ayah
import com.example.kotlin_holy.ui.audio.ReaderAudioViewModel
import com.example.kotlin_holy.ui.components.AudioDownloadCard
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.MemorizationPanel
import com.example.kotlin_holy.ui.components.MushafPageView
import com.example.kotlin_holy.ui.components.PageReadMark
import com.example.kotlin_holy.ui.components.PageTranslation
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.SegmentedControl
import com.example.kotlin_holy.ui.feature.memorization.MemorizationUiState
import com.example.kotlin_holy.ui.feature.memorization.MemorizationViewModel
import com.example.kotlin_holy.ui.theme.HolyFonts
import com.example.kotlin_holy.ui.theme.HolyTheme

private val VIEW_MODES = listOf("ayah" to "Oyat boʻyicha", "page" to "Sahifa boʻyicha")
private val TEXT_MODES = listOf(
    "both" to "Ikkalasi",
    "arabic" to "Arabcha",
    "translation" to "Tarjima",
)

@Composable
fun SurahReadScreen(
    viewModel: SurahReadViewModel = hiltViewModel(),
    audioViewModel: SurahAudioViewModel = hiltViewModel(),
    readerAudioViewModel: ReaderAudioViewModel = hiltViewModel(),
    memorizationViewModel: MemorizationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val readerAudio by readerAudioViewModel.state.collectAsStateWithLifecycle()
    val memo by memorizationViewModel.ui.collectAsStateWithLifecycle()
    val memorizedVerses by memorizationViewModel.progress.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val detail = state.detail

    /*
     * Bir mushaf beti bir nechta qisqa suradan iborat bo'lishi mumkin
     * (masalan 604-bet: Ixlos, Falaq, Nos). "Sahifa bo'yicha" ko'rinishida
     * shu betdagi har bir sura uchun alohida yuklash va ijro kerak —
     * shuning uchun bu rejimda audioViewModel (bitta suraga bog'langan)
     * o'rniga readerAudioViewModel ishlatiladi.
     */
    val pageSurahs = remember(state.group) {
        state.group?.pageContent?.ayahs.orEmpty()
            .map { it.surahNumber to it.surahNameUz }
            .distinctBy { it.first }
    }
    val pageExpectedSizes = remember { mutableStateMapOf<Int, Long>() }
    LaunchedEffect(pageSurahs, readerAudio.downloads.keys) {
        pageSurahs.forEach { (surahNumber, _) ->
            if (!readerAudio.isReady(surahNumber) && pageExpectedSizes[surahNumber] == null) {
                readerAudioViewModel.expectedSize(surahNumber)?.let { pageExpectedSizes[surahNumber] = it }
            }
        }
    }

    /* Oyat raqami bosilganda shu oyat yonida boshqaruv ochiladi */
    var menuVerse by remember { mutableStateOf<String?>(null) }

    /* Mikrofon ruxsati so'ralayotgan vaqtda kutilayotgan oyat matni */
    val micContext = LocalContext.current
    var pendingMicText by remember { mutableStateOf<String?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val text = pendingMicText
        pendingMicText = null
        if (granted && text != null) memorizationViewModel.startListening(text)
    }
    val requestMicAndListen: (String) -> Unit = { expectedText ->
        val granted = ContextCompat.checkSelfPermission(
            micContext,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            memorizationViewModel.startListening(expectedText)
        } else {
            pendingMicText = expectedText
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    /* Yuklashdan oldin hajmni ko'rsatish uchun bir marta so'raladi */
    var expectedBytes by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(audio.surahNumber, audio.downloaded) {
        if (!audio.downloaded && expectedBytes == null) {
            expectedBytes = audioViewModel.expectedSize()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.loading || detail == null) {
            item { LoadingRows(rows = 6) }
            return@LazyColumn
        }

        item {
            HolyCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "${detail.surah.number}. ${detail.surah.nameUz}",
                        color = colors.ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    if (detail.surah.meaningUz != null) {
                        Text(
                            detail.surah.meaningUz,
                            color = colors.accent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        HolyBadge(if (detail.surah.isMakki) "Makkiy" else "Madaniy")
                        HolyBadge(
                            "${detail.surah.totalAyahs} oyat",
                            color = colors.accent,
                            background = colors.accent.copy(alpha = 0.12f),
                        )
                        if (state.xatmTotal > 0) {
                            HolyBadge(
                                text = if (state.xatmRead == state.xatmTotal) {
                                    "✓ Toʻliq oʻqildi"
                                } else {
                                    "${state.xatmRead}/${state.xatmTotal} bet"
                                },
                                color = colors.read,
                                background = colors.read.copy(alpha = 0.12f),
                            )
                        }
                    }
                    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SegmentedControl(VIEW_MODES, state.viewMode, viewModel::setViewMode)
                        SegmentedControl(TEXT_MODES, state.textMode, viewModel::setTextMode)
                    }
                }
            }
        }

        /* ---- Qiroat: yuklash, jarayon yoki o'chirish ----
           "Sahifa bo'yicha" ko'rinishida bet bir nechta suradan iborat
           bo'lishi mumkin, shuning uchun har bir sura uchun alohida karta
           chiqadi; "Oyat bo'yicha"da esa faqat ochiq turgan sura uchun. */
        if (state.viewMode == "page") {
            items(pageSurahs.size) { index ->
                val (surahNumber, surahName) = pageSurahs[index]
                AudioDownloadCard(
                    state = readerAudio.downloadOf(surahNumber),
                    expectedBytes = pageExpectedSizes[surahNumber],
                    title = if (pageSurahs.size > 1) "${surahName ?: "$surahNumber-sura"} qiroati" else null,
                    onDownload = { readerAudioViewModel.download(surahNumber) },
                    onCancel = { readerAudioViewModel.cancelDownload(surahNumber) },
                    onDelete = { readerAudioViewModel.deleteAudio(surahNumber) },
                )
            }
        } else {
            item {
                AudioDownloadCard(
                    state = audio.download,
                    expectedBytes = expectedBytes,
                    onDownload = audioViewModel::download,
                    onCancel = audioViewModel::cancelDownload,
                    onDelete = audioViewModel::deleteAudio,
                )
            }
        }

        if (state.viewMode == "page") {
            item {
                val group = state.group
                if (group == null) {
                    LoadingRows(rows = 5)
                } else {
                    Column {
                        HolyCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 12.dp)) {
                                Text(
                                    "${group.pageNumber}-sahifa" +
                                        (group.juz?.let { " · $it-juz" } ?: ""),
                                    color = colors.inkFaint,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                                )
                                if (state.showArabic && group.mushaf != null) {
                                    MushafPageView(
                                        page = group.mushaf,
                                        scale = state.arabicScale,
                                        highlightedVerse = state.highlightedVerse,
                                        playingVerse = readerAudio.playingVerse,
                                        playingWord = readerAudio.playingWord,
                                        menuVerse = menuVerse,
                                        isPlaying = readerAudio.isPlayingAyah,
                                        /* Har bir oyat o'z surasining qiroatiga bog'liq */
                                        audioReady = readerAudio.isReady(
                                            menuVerse?.substringBefore(':')?.toIntOrNull()
                                                ?: pageSurahs.firstOrNull()?.first ?: 0,
                                        ),
                                        onVerseClick = viewModel::onVerseClick,
                                        onWordClick = { surahNumber, ayahNumber, wordNumber ->
                                            if (readerAudio.isReady(surahNumber)) {
                                                readerAudioViewModel.playWord(surahNumber, ayahNumber, wordNumber)
                                            }
                                        },
                                        onMenuOpen = { verseKey -> menuVerse = verseKey },
                                        onMenuDismiss = { menuVerse = null },
                                        onPlay = readerAudioViewModel::playAyah,
                                        onPlayFrom = readerAudioViewModel::playFrom,
                                        onPause = readerAudioViewModel::pause,
                                        onStop = {
                                            readerAudioViewModel.stop()
                                            menuVerse = null
                                        },
                                    )
                                }
                                if (state.showArabic && state.showTranslation) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 16.dp)
                                            .height(1.dp)
                                            .background(colors.line),
                                    )
                                }
                                if (state.showTranslation && group.pageContent != null) {
                                    PageTranslation(
                                        ayahs = group.pageContent.ayahs,
                                        scale = state.translationScale,
                                    )
                                }
                            }
                        }

                        PageReadMark(
                            readAt = state.readPages[group.pageNumber],
                            onToggle = viewModel::toggleCurrentPage,
                        )

                        ReaderPager(
                            label = "${group.pageNumber}-sahifa",
                            onPrev = if (state.cursor > 0) ({ viewModel.move(-1) }) else null,
                            onNext = if (state.cursor < state.pageNumbers.lastIndex) {
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
        } else {
            val chunks = state.ayahChunks
            val visible = chunks.getOrNull(state.cursor).orEmpty()
            items(visible.size) { index ->
                val ayah = visible[index]
                AyahCard(
                    ayah = ayah,
                    showArabic = state.showArabic,
                    showTranslation = state.showTranslation,
                    arabicScale = state.arabicScale,
                    translationScale = state.translationScale,
                    audioReady = audio.downloaded && audio.ready,
                    isPlaying = audio.playback.isPlaying &&
                        !audio.playback.singleWord &&
                        audio.playingAyah == ayah.numberInSurah,
                    onPlay = { audioViewModel.playAyah(ayah.numberInSurah) },
                    onPause = audioViewModel::pause,
                    onStop = audioViewModel::stop,
                    memorized = memorizedVerses[ayah.verseKey]?.isMemorized ?: false,
                    memorizing = memo.activeVerseKey == ayah.verseKey,
                    memoState = memo,
                    onToggleMemorize = {
                        if (memo.activeVerseKey == ayah.verseKey) {
                            memorizationViewModel.close()
                        } else {
                            memorizationViewModel.open(ayah.verseKey)
                        }
                    },
                    onMicClick = { requestMicAndListen(ayah.textArabic) },
                    onResetMemorize = memorizationViewModel::resetProgress,
                )
            }
            item {
                ReaderPager(
                    label = "${state.cursor + 1} / ${chunks.size}",
                    onPrev = if (state.cursor > 0) ({ viewModel.move(-1) }) else null,
                    onNext = if (state.cursor < chunks.lastIndex) ({ viewModel.move(1) }) else null,
                    prevLabel = "Oldingi oyatlar",
                    nextLabel = "Keyingi oyatlar",
                )
            }
        }
    }
}

@Composable
private fun AyahCard(
    ayah: Ayah,
    showArabic: Boolean,
    showTranslation: Boolean,
    arabicScale: Float,
    translationScale: Float,
    audioReady: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    memorized: Boolean,
    memorizing: Boolean,
    memoState: MemorizationUiState,
    onToggleMemorize: () -> Unit,
    onMicClick: () -> Unit,
    onResetMemorize: () -> Unit,
) {
    val colors = HolyTheme.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val arabicFamily = androidx.compose.runtime.remember { HolyFonts.uthmani(context.assets) }

    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ayah.numberInSurah.toString(),
                        color = colors.inkSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (ayah.juz != null) {
                    HolyBadge("${ayah.juz}-juz", modifier = Modifier.padding(start = 8.dp))
                }
                if (ayah.pageNumber != null) {
                    HolyBadge("${ayah.pageNumber}-sahifa", modifier = Modifier.padding(start = 6.dp))
                }

                Box(Modifier.weight(1f))

                /* Oyat yodlangan bo'lsa doimiy belgi ko'rsatiladi */
                if (memorized && !memorizing) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Yodlandi",
                        tint = colors.read,
                        modifier = Modifier.size(18.dp).padding(end = 8.dp),
                    )
                }

                /* Ovoz bilan yodlashni ochish/yopish */
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (memorizing) colors.accent else colors.accent.copy(alpha = 0.12f),
                        )
                        .clickable(onClick = onToggleMemorize),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Oyatni yodlash",
                        tint = if (memorizing) colors.bg else colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                }

                /* Qiroat yuklangan bo'lsa, oyatni shu yerdan tinglash mumkin */
                if (audioReady) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isPlaying) colors.accent else colors.accent.copy(alpha = 0.12f),
                            )
                            .clickable { if (isPlaying) onPause() else onPlay() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = if (isPlaying) {
                                "Toʻxtatib turish"
                            } else {
                                "${ayah.numberInSurah}-oyatni oʻqish"
                            },
                            tint = if (isPlaying) colors.bg else colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accent.copy(alpha = 0.12f))
                            .clickable(onClick = onStop),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Toʻxtatish",
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (showArabic) {
                Text(
                    text = ayah.textArabic,
                    fontFamily = arabicFamily,
                    color = colors.ink,
                    fontSize = (24 * arabicScale).sp,
                    lineHeight = (48 * arabicScale).sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
            }

            if (memorizing) {
                MemorizationPanel(
                    state = memoState,
                    onMicClick = onMicClick,
                    onReset = onResetMemorize,
                    onClose = onToggleMemorize,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            if (showTranslation) {
                Text(
                    text = ayah.translationUz ?: "Tarjima kiritilmagan",
                    color = colors.inkSoft,
                    fontSize = (15 * translationScale).sp,
                    lineHeight = (26 * translationScale).sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                if (ayah.transcription != null) {
                    Text(
                        text = ayah.transcription,
                        color = colors.inkFaint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

