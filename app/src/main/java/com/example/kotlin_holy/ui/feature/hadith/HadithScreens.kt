package com.example.kotlin_holy.ui.feature.hadith

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.kotlin_holy.domain.model.Hadith
import com.example.kotlin_holy.domain.model.HadithCollection
import com.example.kotlin_holy.domain.model.HadithSection
import com.example.kotlin_holy.domain.model.hadithReminderSource
import com.example.kotlin_holy.domain.model.numberLabel
import com.example.kotlin_holy.domain.repository.LibraryRepository
import com.example.kotlin_holy.domain.repository.MarksRepository
import com.example.kotlin_holy.domain.repository.RemindersRepository
import com.example.kotlin_holy.domain.repository.SettingsRepository
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.ListBox
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.PageHeader
import com.example.kotlin_holy.ui.components.PagerAction
import com.example.kotlin_holy.ui.components.ReadMarkButton
import com.example.kotlin_holy.ui.components.ReadStatus
import com.example.kotlin_holy.ui.components.ReadStatusBadge
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.Segmented
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.navigation.BackLink
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.PageWidth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/*
 * Hadislar bo'limi — veb nusxadagi pages/HadithBooksPage.jsx (/books),
 * pages/HadithBookPage.jsx (/book/:id, /book/:id/s/:section) va pages/HadithReadPage.jsx
 * (/book/:id/s/:section/:number).
 */

/** Qur'on o'qish sahifalari bilan umumiy: Ikkalasi / Arabcha / Tarjima */
private val TEXT_MODES = listOf("both" to "Ikkalasi", "arabic" to "Arabcha", "translation" to "Tarjima")

private const val TRANSLATION_NOTICE =
    "Oʻzbekcha tarjima arabcha matndan sunʼiy intellekt yordamida tayyorlangan va hali " +
        "mutaxassis tomonidan tekshirilmagan. Muhim masalalarda ishonchli manbalarga murojaat qiling."

private const val UNTRANSLATED_NOTICE =
    "Bu boʻlim hali tarjima qilinmagan — hozircha faqat arabcha matn koʻrsatiladi."

/* ======================= /books ======================= */

@HiltViewModel
class HadithBooksViewModel @Inject constructor(
    library: LibraryRepository,
    marksRepository: MarksRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _collections = MutableStateFlow<Loaded<List<HadithCollection>>>(Loaded.Loading)
    internal val collections: StateFlow<Loaded<List<HadithCollection>>> = _collections.asStateFlow()

    val marks: StateFlow<Map<String, Long>?> =
        marksRepository.hadithMarks.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Katak / ro'yxat ko'rinishi — barcha ro'yxat sahifalari uchun umumiy */
    val view: StateFlow<String?> =
        settingsRepository.settings.map { it.listView }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { _collections.value = loadCollections(library) }
    }

    fun setView(view: String) {
        viewModelScope.launch { settingsRepository.setListView(view) }
    }
}

/** /books — hadis kitoblari ro'yxati */
@Composable
fun HadithBooksScreen() {
    val viewModel: HadithBooksViewModel = hiltViewModel()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val marksState by viewModel.marks.collectAsStateWithLifecycle()
    val viewState by viewModel.view.collectAsStateWithLifecycle()
    val nav = LocalNav.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        /* grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 */
        val columns = when {
            maxWidth >= 1280.dp -> 3
            maxWidth >= 640.dp -> 2
            else -> 1
        }
        val list = (collections as? Loaded.Ready)?.value
        val marks = marksState
        val view = viewState
        val error = (collections as? Loaded.Failed)?.message

        HolyPage(maxWidth = PageWidth.Section) {
            item(key = "header") {
                PageHeader(
                    title = "Hadislar",
                    subtitle = "Hadis toʻplamlari — arabcha matn va oʻzbekcha tarjima",
                    trailing = if (list != null && view != null) {
                        { ViewToggle(view = view, onChange = viewModel::setView) }
                    } else {
                        null
                    },
                )
            }
            if (error != null) item(key = "error") { AlertBox(error) }

            if (list == null || marks == null || view == null) {
                if (error == null) item(key = "loading") { LoadingRows(rows = 2, modifier = Modifier.padding(8.dp)) }
            } else if (view == "list") {
                item(key = "list") {
                    ListBox {
                        list.forEachIndexed { index, collection ->
                            BookRow(index, collection, collectionReadStatus(marks, collection)) {
                                nav.open(Routes.book(collection.id))
                            }
                        }
                    }
                }
            } else {
                gridItems(list, columns, gap = 16.dp) { index, collection, modifier, fill ->
                    BookCard(
                        collection = collection,
                        status = collectionReadStatus(marks, collection),
                        modifier = modifier.then(riseIn(index)),
                        fill = fill,
                    ) { nav.open(Routes.book(collection.id)) }
                }
            }
        }
    }
}

/* ======================= /book/:id, /book/:id/s/:section ======================= */

@HiltViewModel
class HadithBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val library: LibraryRepository,
    private val marksRepository: MarksRepository,
    remindersRepository: RemindersRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val id: String = savedStateHandle.get<String>("id").orEmpty()

    /** Yo'ldagi bo'lim (/book/:id/s/:section); /book/:id da null */
    val sectionArg: String? = savedStateHandle.get<String>("section")

    private val _meta = MutableStateFlow<Loaded<BookMeta>>(Loaded.Loading)
    internal val meta: StateFlow<Loaded<BookMeta>> = _meta.asStateFlow()

    private val _hadiths = MutableStateFlow<Loaded<List<Hadith>>>(Loaded.Loading)
    internal val hadiths: StateFlow<Loaded<List<Hadith>>> = _hadiths.asStateFlow()

    val marks: StateFlow<Map<String, Long>?> =
        marksRepository.hadithMarks.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Eslatmalarga saqlangan hadislar — ro'yxat va kartalarda belgi ko'rsatish uchun */
    val savedSources: StateFlow<Set<String>?> = remindersRepository.reminders
        .map { list -> list.mapNotNull { it.source }.filter { it.startsWith("hadith:") }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val view: StateFlow<String?> =
        settingsRepository.settings.map { it.listView }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val meta = loadBookMeta(library, id)
            _meta.value = meta
            val section = (meta as? Loaded.Ready)?.value?.let { openedSection(it.sections) }
            if (section != null) _hadiths.value = loadSectionHadiths(library, id, section)
        }
    }

    /** Ochiladigan bo'lim: yo'ldagisi yoki bitta bo'limli kitobda o'sha bo'lim; null — bo'limlar to'ri */
    fun openedSection(sections: List<HadithSection>): String? =
        sectionArg ?: sections.singleOrNull()?.number?.toString()

    fun setView(view: String) {
        viewModelScope.launch { settingsRepository.setListView(view) }
    }

    fun toggleHadith(number: Double) {
        viewModelScope.launch { marksRepository.toggleHadith(id, number) }
    }

    /** Butun bo'lim: bo'lim hadislari bosilganda yuklanadi (bir marta, keyin keshdan) */
    fun toggleSection(section: HadithSection, read: Boolean) {
        viewModelScope.launch {
            val hadiths = loadSectionHadiths(library, id, section.number.toString())
            /* bo'lim fayli yuklanmadi — belgi o'zgarmaydi */
            if (hadiths is Loaded.Ready) marksRepository.setHadiths(id, hadiths.value.map { it.number }, read)
        }
    }
}

/** /book/:id — bo'limlar to'ri (yoki bitta bo'limli kitobda hadislar), /book/:id/s/:section — hadislar */
@Composable
fun HadithBookScreen() {
    val viewModel: HadithBookViewModel = hiltViewModel()
    val metaState by viewModel.meta.collectAsStateWithLifecycle()
    val hadithsState by viewModel.hadiths.collectAsStateWithLifecycle()
    val marksState by viewModel.marks.collectAsStateWithLifecycle()
    val savedState by viewModel.savedSources.collectAsStateWithLifecycle()
    val viewState by viewModel.view.collectAsStateWithLifecycle()
    val nav = LocalNav.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        /* Suralar ro'yxati bilan bir xil to'r: tor ekranda 1, sm da 2, keng ekranda (75rem) 3 ustun */
        val columns = when {
            maxWidth >= 1200.dp -> 3
            maxWidth >= 640.dp -> 2
            else -> 1
        }
        val layout = BookLayout(columns = columns, sm = maxWidth >= 640.dp, md = maxWidth >= 768.dp)
        val meta = metaState
        val marks = marksState
        val saved = savedState
        val view = viewState

        when {
            meta is Loaded.Failed -> HolyPage(maxWidth = PageWidth.Section) {
                item(key = "error") { ErrorWithLink(meta.message, "← Hadislar") { nav.open(Routes.BOOKS) } }
            }

            meta !is Loaded.Ready || marks == null || saved == null || view == null ->
                HolyPage(maxWidth = PageWidth.Section) { item(key = "loading") { GridSkeleton(columns) } }

            else -> {
                val (collection, sections) = meta.value
                val sectionNumber = viewModel.openedSection(sections)
                if (sectionNumber == null) {
                    SectionGrid(collection, sections, marks, view, layout, viewModel)
                } else {
                    HadithGrid(collection, sections, sectionNumber, hadithsState, marks, saved, view, layout, viewModel)
                }
            }
        }
    }
}

/** Ekran kengligiga bog'liq o'lchamlar */
private data class BookLayout(val columns: Int, val sm: Boolean, val md: Boolean)

/** Kitobning bo'limlari (kitoblari) to'ri — ustida kitob haqida ma'lumot */
@Composable
private fun SectionGrid(
    collection: HadithCollection,
    sections: List<HadithSection>,
    marks: Map<String, Long>,
    view: String,
    layout: BookLayout,
    viewModel: HadithBookViewModel,
) {
    val nav = LocalNav.current
    /* Har bir bo'lim nechta hadisi belgilangan — suralar ro'yxatidagi kabi */
    val statuses = remember(marks, collection.id, sections) {
        val numbers = markedNumbers(marks, collection.id)
        sections.associate { it.number to sectionReadStatus(numbers, it) }
    }
    val bookStatus = collectionReadStatus(marks, collection)
    fun statusOf(section: HadithSection) = statuses[section.number] ?: ReadStatus(0, section.totalHadiths)
    fun open(section: HadithSection) = nav.open(Routes.bookSection(collection.id, section.number))
    fun toggle(section: HadithSection) = viewModel.toggleSection(section, !statusOf(section).done)

    BackLink(Routes.BOOKS, "Hadislar")

    HolyPage(maxWidth = PageWidth.Section) {
        item(key = "info") { HadithBookInfo(collection, bookStatus, layout.sm) }
        item(key = "view") { ViewBar(view, viewModel::setView) }

        if (view == "list") {
            items(sections.size, contentType = { "section-row" }) { index ->
                val section = sections[index]
                SectionRow(
                    index = index,
                    count = sections.size,
                    section = section,
                    status = statusOf(section),
                    sm = layout.sm,
                    onClick = { open(section) },
                    onToggle = { toggle(section) },
                )
            }
        } else {
            gridItems(sections, layout.columns, gap = 12.dp) { index, section, modifier, _ ->
                SectionTile(
                    section = section,
                    status = statusOf(section),
                    modifier = modifier.then(riseIn(index)),
                    onClick = { open(section) },
                    onToggle = { toggle(section) },
                )
            }
        }
    }
}

/** Bitta bo'limdagi hadislar to'ri */
@Composable
private fun HadithGrid(
    collection: HadithCollection,
    sections: List<HadithSection>,
    sectionNumber: String,
    hadiths: Loaded<List<Hadith>>,
    marks: Map<String, Long>,
    saved: Set<String>,
    view: String,
    layout: BookLayout,
    viewModel: HadithBookViewModel,
) {
    val nav = LocalNav.current
    val single = sections.size == 1
    val section = sections.firstOrNull { it.number.toString() == sectionNumber }
    val list = (hadiths as? Loaded.Ready)?.value

    if (list != null && section != null) {
        if (single) BackLink(Routes.BOOKS, "Hadislar") else BackLink(Routes.book(collection.id), collection.nameUz)
    }

    HolyPage(maxWidth = PageWidth.Section) {
        when {
            hadiths is Loaded.Failed -> item(key = "error") { AlertBox(hadiths.message) }
            list == null -> item(key = "loading") { GridSkeleton(layout.columns) }
            section == null -> item(key = "missing") { AlertBox("$sectionNumber-boʻlim topilmadi") }
            else -> {
                val translated = list.count { !it.translation.isNullOrEmpty() }
                val read = list.count { marks[hadithKey(collection.id, it.number)] != null }
                val status = ReadStatus(read, list.size)
                fun readAt(hadith: Hadith) = marks[hadithKey(collection.id, hadith.number)]
                fun isSaved(hadith: Hadith) = hadithReminderSource(collection.id, hadith.number) in saved
                fun open(hadith: Hadith) = nav.open(Routes.hadith(collection.id, section.number, hadith.label))

                item(key = "header") {
                    /* Bitta bo'limli kitob ochilganda darhol hadislar chiqadi — ustida kitob haqida ma'lumot */
                    if (single) {
                        HadithBookInfo(collection, collectionReadStatus(marks, collection), layout.sm)
                    } else {
                        PageHeader(
                            title = sectionTitle(section),
                            subtitle = "${collection.nameUz} · ${section.number}-boʻlim · ${list.size} ta hadis",
                            trailing = if (status.read > 0) {
                                { ReadStatusBadge(status) }
                            } else {
                                null
                            },
                        )
                    }
                }
                item(key = "notice") {
                    AlertBox(
                        if (translated == 0) UNTRANSLATED_NOTICE else TRANSLATION_NOTICE,
                        modifier = Modifier.padding(bottom = 24.dp),
                        error = false,
                    )
                }
                item(key = "view") { ViewBar(view, viewModel::setView) }

                if (view == "list") {
                    items(list.size, contentType = { "hadith-row" }) { index ->
                        val hadith = list[index]
                        HadithRow(
                            index = index,
                            count = list.size,
                            hadith = hadith,
                            readAt = readAt(hadith),
                            saved = isSaved(hadith),
                            md = layout.md,
                            onClick = { open(hadith) },
                            onToggle = { viewModel.toggleHadith(hadith.number) },
                        )
                    }
                } else {
                    gridItems(list, layout.columns, gap = 12.dp) { index, hadith, modifier, _ ->
                        HadithTile(
                            hadith = hadith,
                            readAt = readAt(hadith),
                            saved = isSaved(hadith),
                            modifier = modifier.then(riseIn(index)),
                            onClick = { open(hadith) },
                            onToggle = { viewModel.toggleHadith(hadith.number) },
                        )
                    }
                }
            }
        }
    }
}

/* ======================= /book/:id/s/:section/:number ======================= */

@HiltViewModel
class HadithReadViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    library: LibraryRepository,
    private val marksRepository: MarksRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val id: String = savedStateHandle.get<String>("id").orEmpty()
    val section: String = savedStateHandle.get<String>("section").orEmpty()
    val number: String = savedStateHandle.get<String>("number").orEmpty()

    private val _meta = MutableStateFlow<Loaded<BookMeta>>(Loaded.Loading)
    internal val meta: StateFlow<Loaded<BookMeta>> = _meta.asStateFlow()

    private val _hadiths = MutableStateFlow<Loaded<List<Hadith>>>(Loaded.Loading)
    internal val hadiths: StateFlow<Loaded<List<Hadith>>> = _hadiths.asStateFlow()

    val marks: StateFlow<Map<String, Long>?> =
        marksRepository.hadithMarks.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Qur'on o'qish sahifalari bilan umumiy matn rejimi */
    val textMode: StateFlow<String?> =
        settingsRepository.settings.map { it.textMode }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch { _meta.value = loadBookMeta(library, id) }
        viewModelScope.launch { _hadiths.value = loadSectionHadiths(library, id, section) }
    }

    fun toggleRead(number: Double) {
        viewModelScope.launch { marksRepository.toggleHadith(id, number) }
    }

    fun setTextMode(mode: String) {
        viewModelScope.launch { settingsRepository.setTextMode(mode) }
    }
}

/** /book/:id/s/:section/:number — bitta hadis, oldingi / keyingi */
@Composable
fun HadithReadScreen() {
    val viewModel: HadithReadViewModel = hiltViewModel()
    val metaState by viewModel.meta.collectAsStateWithLifecycle()
    val hadithsState by viewModel.hadiths.collectAsStateWithLifecycle()
    val marksState by viewModel.marks.collectAsStateWithLifecycle()
    val modeState by viewModel.textMode.collectAsStateWithLifecycle()
    val nav = LocalNav.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sm = maxWidth >= 640.dp
        val md = maxWidth >= 768.dp
        val id = viewModel.id
        val section = viewModel.section
        val number = viewModel.number
        val meta = metaState
        val hadiths = hadithsState
        val marks = marksState
        val mode = modeState
        val error = (meta as? Loaded.Failed)?.message ?: (hadiths as? Loaded.Failed)?.message

        when {
            error != null -> HolyPage(maxWidth = PageWidth.Section) { item(key = "error") { AlertBox(error) } }

            meta !is Loaded.Ready || hadiths !is Loaded.Ready || marks == null || mode == null ->
                HolyPage(maxWidth = PageWidth.Section) {
                    item(key = "loading") { LoadingRows(rows = 2, modifier = Modifier.padding(8.dp)) }
                }

            else -> {
                val (collection, sections) = meta.value
                val list = hadiths.value
                val sectionIndex = sections.indexOfFirst { it.number.toString() == section }
                /* Birlashtirilgan rivoyat (299–301) oralig'idagi har qaysi raqam shu hadisni ochadi */
                val value = number.toDoubleOrNull()
                val index = list.indexOfFirst { item ->
                    item.label == number ||
                        (item.numberTo != null && value != null && value >= item.number && value <= item.numberTo)
                }
                val single = sections.size == 1
                val sectionInt = section.toIntOrNull()
                val backTo = when {
                    single -> Routes.book(id)
                    sectionInt != null -> Routes.bookSection(id, sectionInt)
                    else -> "book/$id/s/$section"
                }
                val backLabel = if (single || sectionIndex < 0) collection.nameUz else sectionTitle(sections[sectionIndex])

                if (index < 0 || sectionInt == null) {
                    HolyPage(maxWidth = PageWidth.Section) {
                        item(key = "missing") { ErrorWithLink("$number-hadis topilmadi", "← $backLabel") { nav.open(backTo) } }
                    }
                } else {
                    val hadith = list[index]
                    val prevSection = if (sectionIndex > 0) sections[sectionIndex - 1] else null
                    val nextSection = if (sectionIndex >= 0) sections.getOrNull(sectionIndex + 1) else null

                    /* Bo'lim chegarasida: oldingi bo'limning oxirgi / keyingi bo'limning birinchi hadisiga o'tadi */
                    val prev = when {
                        index > 0 -> PagerAction("Oldingi hadis") {
                            nav.replace(Routes.hadith(id, sectionInt, list[index - 1].label))
                        }
                        prevSection != null -> PagerAction("Oldingi boʻlim") {
                            nav.replace(Routes.hadith(id, prevSection.number, numberLabel(prevSection.lastHadith)))
                        }
                        else -> null
                    }
                    val next = when {
                        index < list.size - 1 -> PagerAction("Keyingi hadis") {
                            nav.replace(Routes.hadith(id, sectionInt, list[index + 1].label))
                        }
                        nextSection != null -> PagerAction("Keyingi boʻlim") {
                            nav.replace(Routes.hadith(id, nextSection.number, numberLabel(nextSection.firstHadith)))
                        }
                        else -> null
                    }

                    BackLink(backTo, backLabel)

                    HolyPage(maxWidth = PageWidth.Section) {
                        item(key = "tools") {
                            WrapEndRow(gapX = 16.dp, gapY = 8.dp, modifier = Modifier.padding(bottom = 20.dp)) {
                                HadithReminderButton(collection.id, hadith, collectionName = collection.nameUz)
                                Segmented(options = TEXT_MODES, value = mode, onChange = viewModel::setTextMode)
                            }
                        }
                        item(key = "card-${hadith.label}") { HadithCard(hadith, mode, sm, md) }
                        item(key = "mark") {
                            ReadMarkButton(
                                readAt = marks[hadithKey(id, hadith.number)],
                                onToggle = { viewModel.toggleRead(hadith.number) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item(key = "pager") {
                            ReaderPager(prev = prev, next = next, label = "${index + 1} / ${list.size}")
                        }
                    }
                }
            }
        }
    }
}
