package com.example.kotlin_holy.ui.feature.settings

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.data.local.AssetJsonSource
import com.example.kotlin_holy.domain.model.DataMeta
import com.example.kotlin_holy.domain.repository.QuranRepository
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.HolyProgress
import com.example.kotlin_holy.ui.components.HolyTextButton
import com.example.kotlin_holy.ui.components.SectionTitle
import com.example.kotlin_holy.ui.theme.HolyTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject

private const val ARCHIVE_NAME = "quron-data.zip"

/** Arxivga yozilgan fayllar soni */
data class ArchiveProgress(val done: Int, val total: Int) {
    val percent: Int get() = if (total > 0) Math.round(done * 100f / total) else 0
}

data class DataSourceState(
    /** meta.json o'qib bo'lindi (topilgan yoki topilmagan) */
    val loaded: Boolean = false,
    val meta: DataMeta? = null,
    val progress: ArchiveProgress? = null,
    val error: String? = null,
)

/**
 * Ma'lumot manbai: meta.json statistikasi va assets/data ichidagi barcha JSON fayllarni
 * bitta .zip ga yig'ish. Ro'yxat manifest.json dan olinadi — arxiv doim ilova ichidagi
 * fayllar bilan bir xil bo'ladi.
 */
@HiltViewModel
class DataSourceViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    private val assets: AssetJsonSource,
    private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(DataSourceState())
    val state: StateFlow<DataSourceState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val meta = runCatching { quranRepository.meta() }.getOrNull()
            _state.update { it.copy(loaded = true, meta = meta) }
        }
    }

    /** Foydalanuvchi tanlagan faylga (Storage Access Framework) arxivni yozadi */
    fun download(uri: Uri) {
        if (_state.value.progress != null) return
        _state.update { it.copy(error = null, progress = ArchiveProgress(0, 0)) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { writeArchive(uri) }
            } catch (error: CancellationException) {
                /* Sahifadan chiqib ketildi — chala yozilgan faylni qoldirmaymiz */
                discard(uri)
                throw error
            } catch (error: Exception) {
                discard(uri)
                _state.update {
                    it.copy(error = error.message?.takeIf(String::isNotBlank) ?: "Arxivni yaratib boʻlmadi")
                }
            } finally {
                _state.update { it.copy(progress = null) }
            }
        }
    }

    private fun discard(uri: Uri) {
        runCatching { DocumentsContract.deleteDocument(context.contentResolver, uri) }
    }

    private suspend fun writeArchive(uri: Uri) {
        val paths = archivePaths()
        _state.update { it.copy(progress = ArchiveProgress(0, paths.size)) }
        val output = context.contentResolver.openOutputStream(uri)
            ?: throw IOException("Tanlangan faylga yozib boʻlmadi")
        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
            paths.forEachIndexed { index, path ->
                currentCoroutineContext().ensureActive()
                val bytes = assets.bytes(path)
                zip.putNextEntry(ZipEntry("data/$path"))
                zip.write(bytes)
                zip.closeEntry()
                _state.update { it.copy(progress = ArchiveProgress(index + 1, paths.size)) }
            }
        }
    }

    /** manifest.json + undagi fayllar; manifest bo'lmasa — assets/data papkasi aylanib chiqiladi */
    private suspend fun archivePaths(): List<String> {
        val listed = runCatching { assets.manifestFiles() }.getOrNull()
        if (listed != null) return (listOf("manifest.json") + listed).distinct()
        return walk("data").map { it.removePrefix("data/") }.sorted()
    }

    private fun walk(dir: String): List<String> =
        (context.assets.list(dir) ?: emptyArray()).flatMap { name ->
            val path = "$dir/$name"
            val children = context.assets.list(path)
            when {
                !children.isNullOrEmpty() -> walk(path)
                name.endsWith(".json") -> listOf(path)
                else -> emptyList()
            }
        }
}

/** Sozlamalar → Ma'lumot manbai */
internal fun LazyListScope.dataSourceSettings() {
    item(key = "data-source") { DataSourceSettings() }
}

@Composable
private fun DataSourceSettings() {
    val viewModel: DataSourceViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val meta = state.meta
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) viewModel.download(uri)
    }

    Box(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        SettingsCard(Modifier.widthIn(max = 672.dp)) {
            SectionTitle("Hozirgi holat", trailing = { HolyBadge("JSON fayllar", tone = BadgeTone.Accent) })

            Row(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("JSON fayllar", color = colors.inkSoft, fontSize = 14.sp)
                if (state.loaded) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusDot(if (meta != null) colors.accent else Color(0xFFEF4444))
                        Text(
                            if (meta != null) "tayyor" else "topilmadi",
                            color = colors.ink,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            if (meta != null) {
                Divider(Modifier.padding(top = 20.dp))
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val stats = listOf(
                        "Suralar" to meta.surahs,
                        "Oyatlar" to meta.ayahs,
                        "Sahifalar" to meta.pages,
                        "Mushaf betlari" to meta.mushafPages,
                        "Qiyin soʻzlar" to meta.words,
                        "Fayllar soni" to meta.files,
                    )
                    GridRows(stats, columns = 2, horizontalGap = 24.dp, verticalGap = 10.dp) { (label, value), modifier ->
                        StatRow(label, uzNumber(value), modifier)
                    }
                    if (meta.totalBytes > 0) {
                        StatRow("JSON umumiy hajmi", formatSize(meta.totalBytes), strong = true)
                    }
                }

                DownloadArchive(
                    totalBytes = meta.totalBytes,
                    progress = state.progress,
                    error = state.error,
                    onDownload = { launcher.launch(ARCHIVE_NAME) },
                )
            }

            val code = buildAnnotatedString {
                append("Barcha maʼlumot ilova ichidagi ")
                withStyle(SpanStyle(background = colors.surface2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)) {
                    append("\u00A0assets/data\u00A0")
                }
                append(
                    " papkasidagi JSON fayllardan oʻqiladi, internet kerak emas. Qurʼon matni oʻzgarmagani " +
                        "uchun fayllarni qayta yozish shart emas.",
                )
            }
            Text(
                code,
                color = colors.inkFaint,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }
}

/** «Barcha JSON ma'lumotni yuklab olish» qatori: tayyorlanayotganda foiz va progress */
@Composable
private fun DownloadArchive(
    totalBytes: Long,
    progress: ArchiveProgress?,
    error: String?,
    onDownload: () -> Unit,
) {
    val colors = HolyTheme.colors
    val shape = HolyTheme.shape
    Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(colors.surface2)
                .border(1.dp, colors.line, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(HolyIcons.Download, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Barcha JSON maʼlumotni yuklab olish",
                        color = colors.ink,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (progress != null) {
                            "Arxiv tayyorlanmoqda… ${uzNumber(progress.done)} / ${uzNumber(progress.total)}"
                        } else {
                            ARCHIVE_NAME + if (totalBytes > 0) " · ${formatSize(totalBytes)} JSON" else ""
                        },
                        color = colors.inkFaint,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        style = tabular(),
                    )
                }
                HolyTextButton(
                    if (progress != null) "${progress.percent}%" else "Yuklab olish",
                    onClick = onDownload,
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Sm,
                    enabled = progress == null,
                )
            }
            if (progress != null) {
                HolyProgress(progress.done, progress.total, Modifier.padding(top = 12.dp))
            }
        }
        if (error != null) AlertBox(error, Modifier.padding(top = 12.dp))
    }
}
