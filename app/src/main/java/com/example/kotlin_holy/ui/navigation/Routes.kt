package com.example.kotlin_holy.ui.navigation

/**
 * Ilovadagi yo'nalishlar — veb nusxadagi App.jsx yo'llari bilan bir xil:
 *   /                       → home
 *   /surah/2                → surah/2   (#ayah-5 o'rniga ?ayah=5)
 *   /surah/2/pages          → surah/2/pages
 *   /surah/2/page/5         → surah/2/page/5
 *   /pages, /page/5, /juz, /juz/1, /juz/1/pages, /juz/1/page/5
 *   /books, /book/bukhari, /book/bukhari/s/1, /book/bukhari/s/1/12
 *   /library, /library/hikam, /library/hikam/p/atamalar, /library/hikam/12
 *   /words, /settings?tab=reminders
 */
object Routes {
    const val HOME = "home"
    const val SURAH = "surah/{number}?ayah={ayah}"
    const val SURAH_PAGES = "surah/{number}/pages"
    const val SURAH_PAGE = "surah/{surah}/page/{number}"
    const val PAGES = "pages"
    const val PAGE = "page/{number}"
    const val JUZ_LIST = "juz"
    const val JUZ = "juz/{number}"
    const val JUZ_PAGES = "juz/{number}/pages"
    const val JUZ_PAGE = "juz/{juz}/page/{number}"
    const val BOOKS = "books"
    const val BOOK = "book/{id}"
    const val BOOK_SECTION = "book/{id}/s/{section}"
    const val HADITH = "book/{id}/s/{section}/{number}"
    const val LIBRARY = "library"
    const val LIBRARY_BOOK = "library/{id}"
    const val LIBRARY_TEXT = "library/{id}/p/{slug}"
    const val LIBRARY_ITEM = "library/{id}/{number}"
    const val WORDS = "words"
    const val SETTINGS = "settings?tab={tab}"

    fun surah(number: Int, ayah: Int? = null) = if (ayah != null) "surah/$number?ayah=$ayah" else "surah/$number"
    fun surahPages(number: Int) = "surah/$number/pages"
    fun surahPage(surah: Int, page: Int) = "surah/$surah/page/$page"
    fun page(number: Int) = "page/$number"
    fun juz(number: Int) = "juz/$number"
    fun juzPages(number: Int) = "juz/$number/pages"
    fun juzPage(juz: Int, page: Int) = "juz/$juz/page/$page"
    fun book(id: String) = "book/$id"
    fun bookSection(id: String, section: Int) = "book/$id/s/$section"
    fun hadith(id: String, section: Int, number: String) = "book/$id/s/$section/$number"
    fun libraryBook(id: String) = "library/$id"
    fun libraryText(id: String, slug: String) = "library/$id/p/$slug"
    fun libraryItem(id: String, number: Int) = "library/$id/$number"
    fun settings(tab: String? = null) = if (tab != null) "settings?tab=$tab" else "settings"

    /** O'qish sahifalari — o'qish vaqti faqat shularda hisoblanadi */
    val readingRoutes = setOf(SURAH, SURAH_PAGE, PAGE, JUZ, JUZ_PAGE)

    /** Yon paneldagi bo'limlar: yo'l → nom («Orqaga» va kenglik uchun) */
    val sections = mapOf(
        HOME to "Suralar",
        PAGES to "Sahifalar",
        JUZ_LIST to "Juzlar",
        BOOKS to "Hadislar",
        LIBRARY to "Kitoblar",
        WORDS to "Qiyin soʻzlar",
    )
}
