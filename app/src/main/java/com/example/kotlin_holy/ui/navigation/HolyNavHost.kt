package com.example.kotlin_holy.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kotlin_holy.ui.feature.hadith.HadithBookScreen
import com.example.kotlin_holy.ui.feature.hadith.HadithBooksScreen
import com.example.kotlin_holy.ui.feature.hadith.HadithReadScreen
import com.example.kotlin_holy.ui.feature.home.HomeScreen
import com.example.kotlin_holy.ui.feature.juz.JuzListScreen
import com.example.kotlin_holy.ui.feature.juz.JuzPagesScreen
import com.example.kotlin_holy.ui.feature.juz.JuzReadScreen
import com.example.kotlin_holy.ui.feature.library.LibraryBookScreen
import com.example.kotlin_holy.ui.feature.library.LibraryItemScreen
import com.example.kotlin_holy.ui.feature.library.LibraryScreen
import com.example.kotlin_holy.ui.feature.library.LibraryTextScreen
import com.example.kotlin_holy.ui.feature.pages.PageListScreen
import com.example.kotlin_holy.ui.feature.pages.PageReadScreen
import com.example.kotlin_holy.ui.feature.settings.SettingsScreen
import com.example.kotlin_holy.ui.feature.surah.SurahPagesScreen
import com.example.kotlin_holy.ui.feature.surah.SurahReadScreen
import com.example.kotlin_holy.ui.feature.words.WordsScreen

private fun args(vararg names: String): List<NamedNavArgument> =
    names.map { name -> navArgument(name) { type = NavType.StringType } }

private fun optional(name: String): NamedNavArgument = navArgument(name) {
    type = NavType.StringType
    nullable = true
    defaultValue = null
}

/**
 * Barcha ekranlar shu grafda ulanadi. Ekranlarning ViewModel'lari yo'l qiymatlarini
 * (number, id, section …) SavedStateHandle'dan oladi.
 */
@Composable
fun HolyNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { fadeIn(tween(250)) },
        exitTransition = { fadeOut(tween(150)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.SURAH, args("number") + optional("ayah")) { SurahReadScreen() }
        composable(Routes.SURAH_PAGES, args("number")) { SurahPagesScreen() }
        composable(Routes.SURAH_PAGE, args("surah", "number")) { PageReadScreen() }
        composable(Routes.PAGES) { PageListScreen() }
        composable(Routes.PAGE, args("number")) { PageReadScreen() }
        composable(Routes.JUZ_LIST) { JuzListScreen() }
        composable(Routes.JUZ, args("number")) { JuzReadScreen() }
        composable(Routes.JUZ_PAGES, args("number")) { JuzPagesScreen() }
        composable(Routes.JUZ_PAGE, args("juz", "number")) { PageReadScreen() }
        composable(Routes.BOOKS) { HadithBooksScreen() }
        composable(Routes.BOOK, args("id")) { HadithBookScreen() }
        composable(Routes.BOOK_SECTION, args("id", "section")) { HadithBookScreen() }
        composable(Routes.HADITH, args("id", "section", "number")) { HadithReadScreen() }
        composable(Routes.LIBRARY) { LibraryScreen() }
        composable(Routes.LIBRARY_BOOK, args("id")) { LibraryBookScreen() }
        composable(Routes.LIBRARY_TEXT, args("id", "slug")) { LibraryTextScreen() }
        composable(Routes.LIBRARY_ITEM, args("id", "number")) { LibraryItemScreen() }
        composable(Routes.WORDS) { WordsScreen() }
        composable(Routes.SETTINGS, listOf(optional("tab"))) { SettingsScreen() }
    }
}
