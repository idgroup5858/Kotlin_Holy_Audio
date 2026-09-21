package com.example.kotlin_holy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.kotlin_holy.domain.repository.AppSettings
import com.example.kotlin_holy.ui.feature.juz.JuzListScreen
import com.example.kotlin_holy.ui.feature.juz.JuzReadScreen
import com.example.kotlin_holy.ui.feature.pageread.PageReadScreen
import com.example.kotlin_holy.ui.feature.pages.PageListScreen
import com.example.kotlin_holy.ui.feature.settings.SettingsScreen
import com.example.kotlin_holy.ui.feature.surahread.SurahReadScreen
import com.example.kotlin_holy.ui.feature.surahs.SurahListScreen
import com.example.kotlin_holy.ui.feature.words.WordsScreen

/** Barcha ekranlar shu grafda ulanadi */
@Composable
fun HolyNavHost(
    navController: NavHostController,
    settings: AppSettings,
) {
    NavHost(navController = navController, startDestination = Routes.SURAHS) {

        composable(Routes.SURAHS) {
            SurahListScreen(onOpenSurah = { navController.navigate(Routes.surah(it)) })
        }

        composable(Routes.PAGES) {
            PageListScreen(onOpenPage = { navController.navigate(Routes.page(it)) })
        }

        composable(Routes.JUZ_LIST) {
            JuzListScreen(onOpenJuz = { navController.navigate(Routes.juz(it)) })
        }

        composable(Routes.WORDS) {
            WordsScreen(onOpenSurah = { navController.navigate(Routes.surah(it)) })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen()
        }

        composable(
            route = Routes.SURAH_READ,
            arguments = listOf(navArgument("number") { type = NavType.StringType }),
        ) {
            SurahReadScreen()
        }

        composable(
            route = Routes.PAGE_READ,
            arguments = listOf(navArgument("number") { type = NavType.StringType }),
        ) {
            PageReadScreen(
                onOpenPage = { page ->
                    navController.navigate(Routes.page(page)) {
                        popUpTo(Routes.PAGE_READ) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.JUZ_READ,
            arguments = listOf(navArgument("number") { type = NavType.StringType }),
        ) {
            JuzReadScreen()
        }
    }
}
