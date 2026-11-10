package com.example.kotlin_holy.ui.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.kotlin_holy.domain.model.Book
import com.example.kotlin_holy.domain.model.BookItem
import com.example.kotlin_holy.domain.model.BookPage
import com.example.kotlin_holy.ui.components.AlertBox
import com.example.kotlin_holy.ui.components.BadgeTone
import com.example.kotlin_holy.ui.components.Diamond
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.components.HolyBadge
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.components.ListBox
import com.example.kotlin_holy.ui.components.LoadingRows
import com.example.kotlin_holy.ui.components.PageHeader
import com.example.kotlin_holy.ui.components.PagerAction
import com.example.kotlin_holy.ui.components.ReadMarkButton
import com.example.kotlin_holy.ui.components.ReadStatus
import com.example.kotlin_holy.ui.components.ReadStatusBadge
import com.example.kotlin_holy.ui.components.ReaderPager
import com.example.kotlin_holy.ui.components.Skeleton
import com.example.kotlin_holy.ui.components.ViewToggle
import com.example.kotlin_holy.ui.components.readStatusLabel
import com.example.kotlin_holy.ui.components.readTileBackground
import com.example.kotlin_holy.ui.components.readTileBorder
import com.example.kotlin_holy.ui.components.rowBackground
import com.example.kotlin_holy.ui.navigation.BackLink
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.HolyPage
import com.example.kotlin_holy.ui.shell.PageWidth
import com.example.kotlin_holy.ui.theme.HolyFonts
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.ReadingText

/* Raqamlar ustun bo'lib tursin (tabular-nums) */
private const val TABULAR = "tnum"

/* ---------------- /library ---------------- */

/** /library — kitoblar ro'yxati (hadis to'plamlari alohida: /books) */
@Composable
fun LibraryScreen() {
    val viewModel: LibraryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val books = state.books

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= XL -> 3
            maxWidth >= SM -> 2
            else -> 1
        }
        HolyPage(maxWidth = PageWidth.Section) {
            item(key = "header") {
                PageHeader(
                    title = "Kitoblar",
                    subtitle = "Oʻzbekcha tarjimadagi kitoblar — boʻlaklab oʻqish va belgilash",
                    trailing = if (books != null) {
                        { ViewToggle(view = state.view, onChange = viewModel::setView) }
                    } else {
                        null
                    },
                )
            }
            item(key = "alert") { AlertBox(state.error) }
            if (books == null && state.error == null) {
                item(key = "loader") { LoadingRows(rows = 2, modifier = Modifier.padding(8.dp)) }
            }
            if (books != null) {
                if (state.view == "list") {
                    item(key = "list") {
                        ListBox {
                            books.forEachIndexed { index, book ->
                                BookRow(
                                    book = book,
                                    index = index,
                                    status = bookReadStatus(state.marks, book),
                                    onOpen = { nav.open(Routes.libraryBook(book.id)) },
                                )
                            }
                        }
                    }
                } else {
                    val rows = books.chunked(columns)
                    itemsIndexed(rows, key = { index, _ -> "books:$columns:$index" }) { index, row ->
                        GridRow(row, columns, gap = 16.dp, modifier = Modifier.padding(top = if (index > 0) 16.dp else 0.dp)) { book, modifier, stretched ->
                            BookCard(
                                book = book,
                                status = bookReadStatus(state.marks, book),
                                stretched = stretched,
                                onOpen = { nav.open(Routes.libraryBook(book.id)) },
                                modifier = modifier,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Ro'yxat qatori: raqam · nomi, muallifi, holati · arabcha nomi */
@Composable
private fun BookRow(book: Book, index: Int, status: ReadStatus, onOpen: () -> Unit) {
    val colors = HolyTheme.colors
    val assets = LocalContext.current.assets
    val uthmani = remember(assets) { HolyFonts.uthmani(assets) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (status.done) readTileBackground() else rowBackground(index))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "${index + 1}",
            color = colors.inkFaint,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = TABULAR),
            modifier = Modifier.width(32.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                book.nameUz,
                color = colors.ink,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${book.authorUz} · ${book.totalItems} ta ${book.itemName}",
                color = colors.inkFaint,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (status.read > 0) {
                Text(
                    readStatusLabel(status, book.itemName),
                    color = colors.read,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFeatureSettings = TABULAR),
                )
            }
        }
        Text(
            book.nameArabic,
            style = TextStyle(
                fontFamily = uthmani,
                fontSize = 20.sp,
                lineHeight = 46.sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right,
                color = colors.inkSoft,
            ),
            maxLines = 1,
        )
    }
}

/** Katak: arabcha nomi, nomi, muallifi, tavsifi va belgilar */
@Composable
private fun BookCard(
    book: Book,
    status: ReadStatus,
    stretched: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    val assets = LocalContext.current.assets
    val uthmani = remember(assets) { HolyFonts.uthmani(assets) }
    HolyCard(
        modifier = modifier,
        borderColor = if (status.done) readTileBorder() else null,
        background = if (status.done) readTileBackground() else null,
        onClick = onOpen,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (stretched) Modifier.fillMaxHeight() else Modifier)
                .padding(20.dp),
        ) {
            Text(
                book.nameArabic,
                style = TextStyle(
                    fontFamily = uthmani,
                    fontSize = 24.sp,
                    lineHeight = 33.sp,
                    textDirection = TextDirection.Rtl,
                    textAlign = TextAlign.Right,
                    color = colors.inkSoft,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                book.nameUz,
                color = colors.ink,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(
                book.authorUz,
                color = colors.inkFaint,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                book.descriptionUz.orEmpty(),
                color = colors.inkSoft,
                fontSize = 14.sp,
                lineHeight = 22.75.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .then(if (stretched) Modifier.weight(1f) else Modifier),
            )
            WrapRow(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                HolyBadge("${book.totalItems} ta ${book.itemName}")
                if (status.read > 0) HolyBadge(readStatusLabel(status, book.itemName), tone = BadgeTone.Read)
            }
        }
    }
}

/* ---------------- /library/{id} ---------------- */

/** /library/{id} — kitob haqida, matn sahifalari va bo'laklar (hikmatlar, boblar) to'ri */
@Composable
fun LibraryBookScreen() {
    val viewModel: BookViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val book = state.book
    val content = state.content

    if (book != null && content != null) BackLink(Routes.LIBRARY, "Kitoblar")

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sm = maxWidth >= SM
        val columns = when {
            maxWidth >= BOOK_GRID_3 -> 3
            sm -> 2
            else -> 1
        }
        HolyPage(maxWidth = PageWidth.Section) {
            when {
                state.error != null -> item(key = "error") {
                    NotFound(state.error.orEmpty(), "← Kitoblar") { nav.back(Routes.LIBRARY) }
                }

                book == null || content == null -> gridSkeleton(columns)

                else -> {
                    val items = content.items
                    val status = bookReadStatus(state.marks, book)
                    /* Birinchi belgilanmagan bo'lak — 264 ta hikmat ichidan qolgan joyni qidirmaslik uchun */
                    val nextUnread = items.firstOrNull { state.readAt(it.number) == null }
                    val openItem = { number: Int -> nav.open(Routes.libraryItem(book.id, number)) }

                    item(key = "info") {
                        BookInfo(book, status, nextUnread, sm, onContinue = openItem)
                    }
                    if (!book.noteUz.isNullOrEmpty()) {
                        item(key = "note") {
                            AlertBox(book.noteUz, Modifier.padding(bottom = 24.dp), error = false)
                        }
                    }
                    if (content.pages.isNotEmpty()) {
                        item(key = "pages") {
                            TextPageLinks(content.pages, if (sm) 2 else 1) { slug ->
                                nav.open(Routes.libraryText(book.id, slug))
                            }
                        }
                    }
                    item(key = "items-header") {
                        ItemsHeader(book, items.size, state.view, viewModel::setView)
                    }
                    if (state.view == "list") {
                        itemsIndexed(items, key = { _, item -> "row:${item.number}" }) { index, item ->
                            ItemRow(
                                book = book,
                                item = item,
                                index = index,
                                count = items.size,
                                readAt = state.readAt(item.number),
                                sm = sm,
                                onOpen = { openItem(item.number) },
                                onToggle = viewModel::toggle,
                            )
                        }
                    } else {
                        val rows = items.chunked(columns)
                        itemsIndexed(rows, key = { index, _ -> "tiles:$columns:$index" }) { index, row ->
                            GridRow(row, columns, gap = 12.dp, modifier = Modifier.padding(top = if (index > 0) 12.dp else 0.dp)) { tile, modifier, _ ->
                                ItemTile(
                                    book = book,
                                    item = tile,
                                    readAt = state.readAt(tile.number),
                                    onOpen = { openItem(tile.number) },
                                    onToggle = viewModel::toggle,
                                    modifier = modifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Yuklanayotganda — BOOK_GRID ichida 9 ta katak */
private fun LazyListScope.gridSkeleton(columns: Int) {
    val rows = (0 until 9).chunked(columns)
    itemsIndexed(rows, key = { index, _ -> "skeleton:$columns:$index" }) { index, row ->
        GridRow(row, columns, gap = 12.dp, modifier = Modifier.padding(top = if (index > 0) 12.dp else 0.dp)) { _, modifier, _ ->
            Skeleton(modifier.height(104.dp))
        }
    }
}

/** Kitob haqida: nomi, muallifi, tavsifi, belgilar va «davom etish» — HadithBookInfo kabi */
@Composable
private fun BookInfo(
    book: Book,
    status: ReadStatus,
    nextUnread: BookItem?,
    sm: Boolean,
    onContinue: (Int) -> Unit,
) {
    val colors = HolyTheme.colors
    val assets = LocalContext.current.assets
    val uthmani = remember(assets) { HolyFonts.uthmani(assets) }

    val title: @Composable (Modifier) -> Unit = { modifier ->
        Column(modifier) {
            Text(
                book.nameUz,
                color = colors.ink,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.025).em,
            )
            Text(
                book.authorUz + (book.subtitleUz?.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""),
                color = colors.inkFaint,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
    val arabic: @Composable (Modifier) -> Unit = { modifier ->
        Text(
            book.nameArabic,
            style = TextStyle(
                fontFamily = uthmani,
                fontSize = 30.sp,
                lineHeight = 41.25.sp,
                textDirection = TextDirection.Rtl,
                textAlign = TextAlign.Right,
                color = colors.accent,
            ),
            modifier = modifier,
        )
    }

    HolyCard(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Column(Modifier.fillMaxWidth().padding(if (sm) 24.dp else 20.dp)) {
            if (sm) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    title(Modifier.weight(1f))
                    arabic(Modifier)
                }
            } else {
                /* Tor ekranda arabcha nom tepada (flex-col-reverse) */
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    arabic(Modifier.fillMaxWidth())
                    title(Modifier)
                }
            }

            if (!book.descriptionUz.isNullOrEmpty()) {
                Text(
                    book.descriptionUz,
                    color = colors.inkSoft,
                    fontSize = 15.sp,
                    lineHeight = 24.375.sp,
                    textAlign = TextAlign.Justify,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            WrapRow(Modifier.fillMaxWidth().padding(top = 16.dp), pushLast = nextUnread != null) {
                HolyBadge("${book.totalItems} ta ${book.itemName}")
                ReadStatusBadge(status, book.itemName)
                if (nextUnread != null) {
                    ContinueButton(
                        label = if (status.read > 0) {
                            "Davom etish · ${book.itemLabel(nextUnread.number)}"
                        } else {
                            "Oʻqishni boshlash"
                        },
                        onClick = { onContinue(nextUnread.number) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContinueButton(label: String, onClick: () -> Unit) {
    val shape = HolyTheme.shape
    Row(
        Modifier
            .clip(shape)
            .background(HolyTheme.colors.accent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold)
        Icon(LibraryIcons.ChevronRightBold, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** Muqaddima, Atamalar lug'ati kabi matn sahifalariga havolalar */
@Composable
private fun TextPageLinks(pages: List<BookPage>, columns: Int, onOpen: (String) -> Unit) {
    val colors = HolyTheme.colors
    Column(
        Modifier.fillMaxWidth().padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        pages.chunked(columns).forEach { row ->
            GridRow(row, columns, gap = 12.dp) { page, modifier, _ ->
                HolyCard(modifier, onClick = { onOpen(page.slug) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            Modifier.size(40.dp).clip(HolyTheme.shape).background(colors.surface2),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(LibraryIcons.Document, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(page.title, color = colors.ink, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold)
                            Text(
                                page.summaryUz.orEmpty(),
                                color = colors.inkFaint,
                                fontSize = 13.sp,
                                lineHeight = 19.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** «Hikmatlar 264» sarlavhasi va ko'rinish almashtirgichi */
@Composable
private fun ItemsHeader(book: Book, count: Int, view: String, onView: (String) -> Unit) {
    val colors = HolyTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.weight(1f)) {
            Text(
                book.itemsTitleUz.orEmpty(),
                color = colors.ink,
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.025).em,
                modifier = Modifier.alignByBaseline(),
            )
            Text(
                "$count",
                color = colors.inkFaint,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                style = TextStyle(fontFeatureSettings = TABULAR),
                modifier = Modifier.padding(start = 8.dp).alignByBaseline(),
            )
        }
        ViewToggle(view = view, onChange = onView)
    }
}

/** Katak: belgi doirachasi, raqam rombi, bob nomi va tavsifi (hikmatda — matnining boshi) */
@Composable
private fun ItemTile(
    book: Book,
    item: BookItem,
    readAt: Long?,
    onOpen: () -> Unit,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    HolyCard(
        modifier = modifier,
        borderColor = if (readAt != null) readTileBorder() else null,
        background = if (readAt != null) readTileBackground() else null,
        onClick = onOpen,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Diamond(item.number.toString())
                /* Bobda — nomi va mundarijadagi qisqa tavsifi, hikmatda — matnining boshi */
                Column(Modifier.weight(1f).padding(end = 16.dp)) {
                    Text(
                        item.title ?: item.text.orEmpty(),
                        color = colors.ink,
                        fontSize = 14.sp,
                        lineHeight = 19.25.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (!item.summaryUz.isNullOrEmpty()) {
                        Text(
                            item.summaryUz,
                            color = colors.inkFaint,
                            fontSize = 13.sp,
                            lineHeight = 17.875.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (!item.commentary.isNullOrEmpty()) {
                        HolyBadge("Sharh bilan", Modifier.padding(top = 8.dp))
                    }
                }
            }
            BookItemReadToggle(
                book = book,
                number = item.number,
                readAt = readAt,
                onToggle = onToggle,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            )
        }
    }
}

/** Jadval uslubidagi qator: belgi · raqam · matn boshi (bob nomi va tavsifi) · sharh */
@Composable
private fun ItemRow(
    book: Book,
    item: BookItem,
    index: Int,
    count: Int,
    readAt: Long?,
    sm: Boolean,
    onOpen: () -> Unit,
    onToggle: (Int) -> Unit,
) {
    val colors = HolyTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .listBoxRow(index, count)
            .background(if (readAt != null) readTileBackground() else rowBackground(index))
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            BookItemReadToggle(book, item.number, readAt, onToggle)
        }
        Text(
            "${item.number}",
            color = colors.inkFaint,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(fontFeatureSettings = TABULAR),
            modifier = Modifier.width(40.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                item.title ?: item.text.orEmpty(),
                color = colors.ink,
                fontSize = 15.sp,
                lineHeight = 22.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!item.summaryUz.isNullOrEmpty()) {
                Text(
                    item.summaryUz,
                    color = colors.inkFaint,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        /* «Sharh» ustuni tor ekranda yashirin */
        if (sm && !item.commentary.isNullOrEmpty()) HolyBadge("Sharh")
    }
}

/** Xato va orqaga havola: «Sahifa topilmadi» · «← Al-Hikam» */
@Composable
private fun NotFound(message: String, linkLabel: String, onLink: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AlertBox(message)
        Text(
            linkLabel,
            color = HolyTheme.colors.accent,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clip(HolyTheme.shape).clickable(onClick = onLink),
        )
    }
}

/* ---------------- /library/{id}/p/{slug} ---------------- */

/** /library/{id}/p/{slug} — Muqaddima, Atamalar lug'ati, Kirish kabi matn sahifasi */
@Composable
fun LibraryTextScreen() {
    val viewModel: BookViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val book = state.book
    val content = state.content
    val id = viewModel.id
    val index = content?.pages?.indexOfFirst { it.slug == viewModel.slug } ?: -1

    if (book != null && index >= 0) BackLink(Routes.libraryBook(id), book.nameUz)

    /* Matndagi havola bo'lakni yangi sahifada ochadi — orqaga qaytsa shu matnga keladi */
    val onItem = remember(nav, id) { { number: Int -> nav.open(Routes.libraryItem(id, number)) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sm = maxWidth >= SM
        HolyPage(maxWidth = PageWidth.Reading) {
            when {
                state.error != null -> item(key = "error") { AlertBox(state.error) }

                book == null || content == null -> item(key = "loader") {
                    LoadingRows(rows = 3, modifier = Modifier.padding(8.dp))
                }

                index < 0 -> item(key = "missing") {
                    NotFound("Sahifa topilmadi", "← ${book.nameUz}") { nav.back(Routes.libraryBook(id)) }
                }

                else -> {
                    val pages = content.pages
                    val page = pages[index]
                    val first = content.items.firstOrNull()
                    /* Oxirgi matn sahifasidan keyin — birinchi bo'lak (hikmat, bob) */
                    val prev = if (index > 0) {
                        val target = pages[index - 1]
                        PagerAction(target.title) { nav.replace(Routes.libraryText(id, target.slug)) }
                    } else {
                        null
                    }
                    val next = when {
                        index < pages.lastIndex -> {
                            val target = pages[index + 1]
                            PagerAction(target.title) { nav.replace(Routes.libraryText(id, target.slug)) }
                        }

                        first != null -> PagerAction(book.itemLabel(first.number)) {
                            nav.replace(Routes.libraryItem(id, first.number))
                        }

                        else -> null
                    }

                    item(key = "header") {
                        PageHeader(
                            title = page.title,
                            subtitle = listOfNotNull(book.nameUz, page.summaryUz).joinToString(" · "),
                        )
                    }
                    bookBlocks(page.blocks, book, sm, onItem, keyPrefix = "page:${page.slug}")
                    item(key = "pager") { ReaderPager(prev = prev, next = next, label = null) }
                }
            }
        }
    }
}

/* ---------------- /library/{id}/{number} ---------------- */

/** /library/{id}/{number} — bitta hikmat yoki bob, oldingi / keyingi */
@Composable
fun LibraryItemScreen() {
    val viewModel: BookViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val book = state.book
    val content = state.content
    val id = viewModel.id
    val number = viewModel.number.orEmpty()
    val index = content?.items?.indexOfFirst { it.number.toString() == number } ?: -1

    if (book != null && index >= 0) BackLink(Routes.libraryBook(id), book.nameUz)

    /* Boshqa bo'lakka havola joriy sahifani almashtiradi — ekran yangidan, boshidan ochiladi */
    val onItem = remember(nav, id) { { target: Int -> nav.replace(Routes.libraryItem(id, target)) } }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sm = maxWidth >= SM
        HolyPage(maxWidth = PageWidth.Reading) {
            when {
                state.error != null -> item(key = "error") { AlertBox(state.error) }

                book == null || content == null -> item(key = "loader") {
                    LoadingRows(rows = 2, modifier = Modifier.padding(8.dp))
                }

                index < 0 -> item(key = "missing") {
                    NotFound("$number-${book.itemName} topilmadi", "← ${book.nameUz}") {
                        nav.back(Routes.libraryBook(id))
                    }
                }

                else -> {
                    val items = content.items
                    val current = items[index]
                    val lastPage = content.pages.lastOrNull()
                    /* Birinchi bo'lakdan oldin — kitobning oxirgi matn sahifasi (Atamalar lug'ati, Kirish) */
                    val prev = when {
                        index > 0 -> {
                            val target = items[index - 1].number
                            PagerAction("Oldingi ${book.itemName}") { nav.replace(Routes.libraryItem(id, target)) }
                        }

                        lastPage != null -> PagerAction(lastPage.title) {
                            nav.replace(Routes.libraryText(id, lastPage.slug))
                        }

                        else -> null
                    }
                    val next = if (index < items.lastIndex) {
                        val target = items[index + 1].number
                        PagerAction("Keyingi ${book.itemName}") { nav.replace(Routes.libraryItem(id, target)) }
                    } else {
                        null
                    }

                    val blocks = current.blocks
                    if (blocks != null) {
                        item(key = "chapter") { ChapterHeader(book, current, sm) }
                        bookBlocks(blocks, book, sm, onItem, keyPrefix = "item:${current.number}")
                    } else {
                        item(key = "saying") { SayingCard(book, current, onItem) }
                    }
                    item(key = "mark") {
                        ReadMarkButton(
                            readAt = state.readAt(current.number),
                            onToggle = { viewModel.toggle(current.number) },
                        )
                    }
                    item(key = "pager") {
                        ReaderPager(prev = prev, next = next, label = "${index + 1} / ${items.size}")
                    }
                }
            }
        }
    }
}

/** Hikmat: qisqa matn va sharhi — bitta kartochkada */
@Composable
private fun SayingCard(book: Book, item: BookItem, onItem: (Int) -> Unit) {
    val colors = HolyTheme.colors
    HolyCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Diamond(item.number.toString())
                Text(book.itemLabel(item.number), color = colors.inkFaint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            LinkedText(
                text = item.text.orEmpty(),
                book = book,
                style = ReadingText.uz.copy(color = colors.ink),
                onItem = onItem,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )

            if (!item.commentary.isNullOrEmpty()) {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                    Divider()
                    Text(
                        "SHARH",
                        color = colors.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.12.em,
                        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
                    )
                    LinkedText(
                        text = item.commentary,
                        book = book,
                        style = ReadingText.uzSub.copy(color = colors.inkSoft),
                        onItem = onItem,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Bob sarlavhasi: romb, «3-bob · izoh» va nomi; matni (bo'limlar, iqtiboslar, ro'yxatlar) ostida */
@Composable
private fun ChapterHeader(book: Book, item: BookItem, sm: Boolean) {
    val colors = HolyTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(bottom = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Diamond(item.number.toString())
        Column(Modifier.weight(1f)) {
            Text(
                book.itemLabel(item.number) + (item.subtitleUz?.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""),
                color = colors.inkFaint,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                item.title.orEmpty(),
                color = colors.ink,
                fontSize = if (sm) 30.sp else 24.sp,
                lineHeight = if (sm) 37.5.sp else 30.sp,
                letterSpacing = (-0.025).em,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
