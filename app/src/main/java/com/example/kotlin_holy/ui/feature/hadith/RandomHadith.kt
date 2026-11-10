package com.example.kotlin_holy.ui.feature.hadith

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import com.example.kotlin_holy.domain.repository.LibraryRepository
import com.example.kotlin_holy.ui.components.ButtonSize
import com.example.kotlin_holy.ui.components.ButtonVariant
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyButton
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.HolyIcons
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ReadingText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/*
 * Veb nusxadagi components/RandomHadith.jsx: barcha hadis kitoblaridan tasodifiy bitta hadis —
 * faqat o'zbekcha tarjimasi bor, mustaqil ma'noli; tugma bilan almashtiriladi.
 */

/* Faqat oldingi rivoyatga ishora qiluvchi qisqa matnlar («shunga oʻxshash rivoyat qildi»,
   «shu hadisni rivoyat qildi») — yolg'iz o'zi hech narsa anglatmaydi, tasodifiy chiqmaydi */
private val REFERENCE_ONLY = Regex(
    "oʻxshash|mislini|maʼno|xuddi (oʻzini|shunday)|aynan shunday|yuqoridagi|(oldingi|avvalgi) hadis|" +
        "uzilib qolgan|zikr qilmadi|shu hadisni|shu \\(hadis\\)ni|hadisini rivoyat|hadisi kabi|toʻliqroq|" +
        "manbada|^rivoyat qil",
    RegexOption.IGNORE_CASE,
)

private fun isStandalone(hadith: Hadith): Boolean {
    val text = hadith.textUz?.trim().orEmpty()
    return text.length >= 25 && !(text.length < 120 && REFERENCE_ONLY.containsMatchIn(text))
}

private fun <T> pick(items: List<T>): T = items[Random.nextInt(items.size)]

/* Bo'lim hadislar soniga qarab tanlanadi — kitob ichidagi har bir hadis bir xil ehtimol bilan chiqadi */
private fun <T> pickWeighted(items: List<T>, weight: (T) -> Int): T {
    var rest = Random.nextDouble() * items.sumOf(weight)
    return items.firstOrNull { item ->
        rest -= weight(item)
        rest < 0
    } ?: items.last()
}

/** Tanlangan hadis va u qaysi kitob / bo'limdan */
data class RandomHadithItem(
    val collection: HadithCollection,
    val section: HadithSection,
    val hadith: Hadith,
)

data class RandomHadithState(
    val item: RandomHadithItem? = null,
    val error: String = "",
    val loading: Boolean = true,
)

private class NoHadithException : Exception("Hadis topilmadi")

@HiltViewModel
class RandomHadithViewModel @Inject constructor(
    private val library: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RandomHadithState())
    val state: StateFlow<RandomHadithState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    /** Boshqa hadis: eski hadis yangisi kelguncha xira holda turadi */
    fun refresh() {
        _state.update { it.copy(loading = true) }
        load()
    }

    private fun load() {
        job?.cancel()
        job = viewModelScope.launch {
            try {
                _state.value = RandomHadithState(item = loadRandomHadith(), error = "", loading = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(error = error.message ?: "Nomaʼlum xatolik", loading = false) }
            }
        }
    }

    /* Avval kitob (har biri teng ehtimol bilan — kichik to'plamlar ham chiqib tursin), so'ng bo'lim va hadis */
    private suspend fun loadRandomHadith(): RandomHadithItem {
        val collections = loadCollections(library).orThrow().filter { it.translatedHadiths > 0 }
        if (collections.isEmpty()) throw NoHadithException()
        repeat(5) {
            val collection = pick(collections)
            val sections = loadBookMeta(library, collection.id).orThrow().sections.filter { it.translatedHadiths > 0 }
            if (sections.isEmpty()) return@repeat
            val section = pickWeighted(sections) { it.translatedHadiths }
            val hadiths = loadSectionHadiths(library, collection.id, section.number.toString())
                .orThrow()
                .filter(::isStandalone)
            if (hadiths.isNotEmpty()) return RandomHadithItem(collection, section, pick(hadiths))
        }
        throw NoHadithException()
    }

    private fun <T> Loaded<T>.orThrow(): T = when (this) {
        is Loaded.Ready -> value
        is Loaded.Failed -> throw IllegalStateException(message)
        Loaded.Loading -> throw IllegalStateException("Nomaʼlum xatolik")
    }
}

/* Veb nusxadagi «-my-1.5 -mr-2»: tugma qator balandligini oshirmaydi, o'ng chetga yopishadi */
private fun Modifier.negativeMargin(vertical: Dp, end: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val v = vertical.roundToPx()
    val e = end.roundToPx()
    layout((placeable.width - e).coerceAtLeast(0), (placeable.height - 2 * v).coerceAtLeast(0)) {
        placeable.place(0, -v)
    }
}

/** Yangilash belgisi — yuklanayotganda aylanadi (animate-spin) */
@Composable
private fun RefreshIcon(spinning: Boolean) {
    val angle = if (spinning) {
        val transition = rememberInfiniteTransition(label = "spin")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
            label = "spin-angle",
        )
        value
    } else {
        0f
    }
    Icon(
        HolyIcons.Refresh,
        contentDescription = "Boshqa tasodifiy hadis",
        tint = LocalContentColor.current,
        modifier = Modifier.size(16.dp).rotate(angle),
    )
}

/**
 * Barcha hadis kitoblaridan tasodifiy bitta hadis — bosh sahifadagi «Tasodifiy hadis».
 * Sarlavhadagi havola hadis sahifasini ochadi, o'ngdagi tugma boshqa hadis tanlaydi.
 */
@Composable
fun RandomHadithCard(modifier: Modifier = Modifier) {
    val viewModel: RandomHadithViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = HolyTheme.colors
    val nav = LocalNav.current
    val sm = LocalConfiguration.current.screenWidthDp >= 640
    val item = state.item
    val hadith = item?.hadith
    val dim by animateFloatAsState(if (state.loading) 0.5f else 1f, tween(150), label = "dim")

    HolyCard(modifier.fillMaxWidth().then(fadeIn(Unit))) {
        Column(Modifier.padding(horizontal = if (sm) 20.dp else 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    itemVerticalAlignment = Alignment.CenterVertically,
                ) {
                    HolyBadge("Hadis")
                    if (item != null && hadith != null) {
                        Text(
                            "${item.collection.nameUz} · ${hadith.rangeLabel}-hadis",
                            color = colors.inkFaint,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .clip(HolyTheme.shape)
                                .clickable {
                                    nav.open(Routes.hadith(item.collection.id, item.section.number, hadith.label))
                                },
                        )
                    }
                    if (hadith?.translation == "ai") HolyBadge("Sunʼiy intellekt tarjimasi")
                }
                HolyButton(
                    onClick = viewModel::refresh,
                    variant = ButtonVariant.Ghost,
                    size = ButtonSize.Icon,
                    enabled = !state.loading,
                    modifier = Modifier.negativeMargin(vertical = 6.dp, end = 8.dp),
                ) {
                    RefreshIcon(spinning = state.loading)
                }
            }

            when {
                item != null && hadith != null -> Column(
                    Modifier
                        .padding(top = 8.dp)
                        .then(fadeIn("${item.collection.id}/${hadith.label}"))
                        .alpha(dim),
                ) {
                    if (!hadith.narratorUz.isNullOrEmpty()) {
                        Text(
                            hadith.narratorUz,
                            style = ReadingText.uzSub.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                    Text(hadith.textUz.orEmpty(), style = ReadingText.uz)
                    if (!hadith.sourceUz.isNullOrEmpty()) {
                        Text(hadith.sourceUz, style = ReadingText.uzSub, modifier = Modifier.padding(top = 12.dp))
                    }
                }

                state.error.isNotEmpty() -> Text(
                    state.error,
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp),
                )

                else -> Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skeleton(Modifier.fillMaxWidth(2f / 3f).height(16.dp))
                    Skeleton(Modifier.fillMaxWidth().height(16.dp))
                    Skeleton(Modifier.fillMaxWidth(5f / 6f).height(16.dp))
                }
            }
        }
    }
}
