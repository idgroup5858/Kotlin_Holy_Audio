package com.example.kotlin_holy.ui.feature.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Book
import com.example.kotlin_holy.domain.model.BookContent
import com.example.kotlin_holy.domain.repository.LibraryRepository
import com.example.kotlin_holy.domain.repository.MarksRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.ui.components.ReadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/* ---------------- Belgilar (lib/bookMarks.js) ---------------- */

/** «hikam:12» — o'qildi belgisining kaliti */
internal fun bookItemKey(bookId: String, number: Int) = "$bookId:$number"

/** Butun kitob holati: { read, total, done } */
internal fun bookReadStatus(marks: Map<String, Long>, book: Book): ReadStatus {
    val prefix = "${book.id}:"
    return ReadStatus(read = marks.keys.count { it.startsWith(prefix) }, total = book.totalItems)
}

/** Yuklash xatosi — matni sahifada Alert sifatida ko'rsatiladi */
private class LoadError(message: String) : Exception(message)

/* ---------------- /library ---------------- */

data class LibraryUiState(
    val books: List<Book>? = null,
    val error: String? = null,
    val marks: Map<String, Long> = emptyMap(),
    /** "grid" | "list" — barcha ro'yxat sahifalari uchun umumiy */
    val view: String = "grid",
)

/** /library — kitoblar ro'yxati */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    marks: MarksRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val books = MutableStateFlow<Result<List<Book>>?>(null)

    val state: StateFlow<LibraryUiState> = combine(
        books,
        marks.bookMarks,
        settings.settings.map { it.listView }.distinctUntilChanged(),
    ) { loaded, marked, view ->
        LibraryUiState(
            books = loaded?.getOrNull(),
            error = loaded?.exceptionOrNull()?.message,
            marks = marked,
            view = if (view == "list") "list" else "grid",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    init {
        viewModelScope.launch {
            books.value = try {
                Result.success(library.books())
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Result.failure(LoadError("books.json topilmadi"))
            }
        }
    }

    fun setView(view: String) {
        viewModelScope.launch { settings.setListView(view) }
    }
}

/* ---------------- /library/{id}, /library/{id}/p/{slug}, /library/{id}/{number} ---------------- */

data class BookUiState(
    val book: Book? = null,
    val content: BookContent? = null,
    val error: String? = null,
    val marks: Map<String, Long> = emptyMap(),
    val view: String = "grid",
) {
    val loading: Boolean get() = error == null && (book == null || content == null)

    fun readAt(number: Int): Long? = book?.let { marks[bookItemKey(it.id, number)] }
}

private data class BookData(val book: Book, val content: BookContent)

/**
 * Kitob ma'lumoti (books.json dan) va matni (<id>.json) — lib/books.js dagi useBook.
 * Kitob sahifasi, matn sahifasi va bo'lak sahifasi uchun umumiy; [slug] va [number] —
 * yo'ldagi qiymatlar (bo'lmasa null).
 */
@HiltViewModel
class BookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val library: LibraryRepository,
    private val marks: MarksRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    val id: String = savedStateHandle.get<String>("id").orEmpty()
    val slug: String? = savedStateHandle.get<String>("slug")
    val number: String? = savedStateHandle.get<String>("number")

    private val data = MutableStateFlow<Result<BookData>?>(null)

    val state: StateFlow<BookUiState> = combine(
        data,
        marks.bookMarks,
        settings.settings.map { it.listView }.distinctUntilChanged(),
    ) { loaded, marked, view ->
        BookUiState(
            book = loaded?.getOrNull()?.book,
            content = loaded?.getOrNull()?.content,
            error = loaded?.exceptionOrNull()?.message,
            marks = marked,
            view = if (view == "list") "list" else "grid",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BookUiState())

    init {
        viewModelScope.launch {
            data.value = try {
                Result.success(load())
            } catch (error: CancellationException) {
                throw error
            } catch (error: LoadError) {
                Result.failure(error)
            } catch (error: Exception) {
                Result.failure(LoadError(error.message ?: "Nomaʼlum xatolik"))
            }
        }
    }

    private suspend fun load(): BookData {
        val book = try {
            library.book(id)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw LoadError("books.json topilmadi")
        } ?: throw LoadError("\"$id\" kitobi topilmadi")
        val content = try {
            library.bookContent(id)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            throw LoadError("$id.json topilmadi")
        }
        return BookData(book, content)
    }

    /** Bo'lakni o'qildi deb belgilaydi yoki belgini olib tashlaydi */
    fun toggle(number: Int) {
        viewModelScope.launch { marks.toggleBookItem(id, number) }
    }

    fun setView(view: String) {
        viewModelScope.launch { settings.setListView(view) }
    }
}
