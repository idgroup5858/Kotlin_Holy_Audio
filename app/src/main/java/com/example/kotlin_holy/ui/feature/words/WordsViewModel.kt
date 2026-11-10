package com.example.kotlin_holy.ui.feature.words

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale
import javax.inject.Inject

/* Bir sahifadagi so'zlar soni */
internal const val PAGE_SIZE = 24

/**
 * words.json dagi so'z — umumiy DifficultWord'da qiyinlik (difficulty) va harakatsiz
 * yozuvi (textPlain) yo'q, ular faqat shu sahifada kerak, shuning uchun fayl shu yerda o'qiladi.
 */
@Serializable
data class WordEntry(
    val id: Int,
    val text: String,
    val textPlain: String? = null,
    val transliteration: String? = null,
    val count: Int = 0,
    val difficulty: Int = 0,
    val firstSurah: Int? = null,
    val firstAyah: Int? = null,
) {
    /** Hisoblagich bo'lmasa ham so'z kamida bir marta uchragan */
    val times: Int get() = if (count > 1) count else 1
}

@Serializable
private data class WordsFile(val items: List<WordEntry> = emptyList())

private data class Query(val search: String = "", val pageIndex: Int = 0)

data class WordsUiState(
    /** 350 ms kechikib qo'llangan qidiruv */
    val search: String = "",
    val pageIndex: Int = 0,
    /** Qidiruvga mos barcha so'zlar */
    val matches: List<WordEntry> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val view: String = "grid",
) {
    val total: Int get() = matches.size
    val totalPages: Int get() = maxOf(1, (total + PAGE_SIZE - 1) / PAGE_SIZE)
    val items: List<WordEntry>
        get() = matches.drop(pageIndex * PAGE_SIZE).take(PAGE_SIZE)
}

/** /words — Qur'ondagi o'qilishi qiyin so'zlar: qidiruv va sahifalash */
@HiltViewModel
class WordsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val words = MutableStateFlow<Result<List<WordEntry>>?>(null)
    private val query = MutableStateFlow(Query())
    private var debounce: Job? = null

    /** Qidiruv maydonidagi matn — kiritish maydoni bilan darhol (kechikmasdan) bog'langan */
    var queryText by mutableStateOf("")
        private set

    val state: StateFlow<WordsUiState> = combine(
        words,
        query,
        settings.settings.map { it.listView }.distinctUntilChanged(),
    ) { loaded, current, view ->
        val all = loaded?.getOrNull().orEmpty()
        val needle = current.search.lowercase(Locale.ROOT)
        val matches = if (needle.isEmpty()) {
            all
        } else {
            all.filter { word ->
                listOfNotNull(word.text, word.textPlain, word.transliteration)
                    .filter { it.isNotEmpty() }
                    .any { it.lowercase(Locale.ROOT).contains(needle) }
            }
        }
        WordsUiState(
            search = current.search,
            pageIndex = current.pageIndex,
            matches = matches,
            loading = loaded == null,
            error = loaded?.exceptionOrNull()?.message,
            view = if (view == "list") "list" else "grid",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WordsUiState())

    init {
        viewModelScope.launch {
            words.value = try {
                Result.success(read())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(Exception("words.json topilmadi"))
            }
        }
    }

    private suspend fun read(): List<WordEntry> = withContext(Dispatchers.IO) {
        context.assets.open("data/words.json").use { stream ->
            json.decodeFromString<WordsFile>(stream.readBytes().decodeToString()).items
        }
    }

    /* Qidiruvni biroz kechiktiramiz — har harfda ro'yxat qayta hisoblanmasin */
    fun setQuery(text: String) {
        queryText = text
        debounce?.cancel()
        debounce = viewModelScope.launch {
            delay(350)
            val search = text.trim()
            query.update { if (it.search == search) it else it.copy(search = search, pageIndex = 0) }
        }
    }

    fun movePage(delta: Int) {
        val totalPages = state.value.totalPages
        query.update { it.copy(pageIndex = (it.pageIndex + delta).coerceIn(0, totalPages - 1)) }
    }

    fun setView(view: String) {
        viewModelScope.launch { settings.setListView(view) }
    }
}
