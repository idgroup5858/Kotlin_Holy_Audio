package com.example.kotlin_holy.ui.navigation

/** Ilovadagi barcha yo'nalishlar shu yerda to'planadi */
object Routes {
    const val SURAHS = "surahs"
    const val PAGES = "pages"
    const val JUZ_LIST = "juz"
    const val WORDS = "words"
    const val SETTINGS = "settings"
    const val SEARCH = "search"

    const val SURAH_READ = "surah/{number}"
    const val PAGE_READ = "page/{number}"
    const val JUZ_READ = "juz/{number}"

    fun surah(number: Int) = "surah/$number"
    fun page(number: Int) = "page/$number"
    fun juz(number: Int) = "juz/$number"

    /** Pastki menyudagi bo'limlar */
    val bottomTabs = listOf(SURAHS, PAGES, JUZ_LIST, WORDS, SETTINGS)
}
