package com.example.kotlin_holy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

/**
 * Ekranlar navigatsiyaga shu orqali murojaat qiladi: veb nusxadagi <Link to="…">.
 * [back] — «‹ Orqaga» havolasi: kerakli sahifa orqada turgan bo'lsa unga qaytadi,
 * aks holda yangidan ochadi (orqaga tugmasi zanjiri cho'zilib ketmasligi uchun).
 */
@Stable
class NavActions(private val controller: NavHostController) {

    /**
     * Yo'l aynan hozirgi sahifa bo'lsa hech narsa qilinmaydi. Bir xil ekran boshqa qiymat
     * bilan (surah/2 → surah/3) yangi sahifa bo'lib ochiladi — launchSingleTop eski
     * ViewModel'ni eski qiymatlari bilan qoldirib yuborardi.
     */
    fun open(route: String) {
        if (route == currentRoute()) return
        controller.navigate(route)
    }

    /** Joriy sahifaning to'ldirilgan yo'li: "surah/2?ayah=5", "page/12" */
    private fun currentRoute(): String? {
        val entry = controller.currentBackStackEntry ?: return null
        val pattern = entry.destination.route ?: return null
        var result = pattern.substringBefore('?')
        entry.destination.arguments.keys.forEach { key ->
            result = result.replace("{$key}", entry.arguments?.getString(key).orEmpty())
        }
        val query = pattern.substringAfter('?', "")
        if (query.isNotEmpty()) {
            val params = query.split('&').mapNotNull { part ->
                val key = part.substringBefore('=')
                entry.arguments?.getString(key)?.let { "$key=$it" }
            }
            if (params.isNotEmpty()) result += "?" + params.joinToString("&")
        }
        return result
    }

    /** Joriy sahifani almashtirib ochadi — bet va hadislar orasida yurganda */
    fun replace(route: String) {
        if (route == currentRoute()) return
        val current = controller.currentBackStackEntry?.destination?.id
        controller.navigate(route) {
            if (current != null) popUpTo(current) { inclusive = true }
        }
    }

    fun back(route: String) {
        if (route == UP) {
            if (!controller.popBackStack()) open(Routes.HOME)
            return
        }
        val popped = runCatching { controller.popBackStack(route, inclusive = false) }.getOrDefault(false)
        if (!popped) replace(route)
    }

    /**
     * Oldingi sahifaning qisqa nomi — Sozlamalardagi «‹ Orqaga» uchun (veb nusxadagi
     * returnPoint.js labelFor): «Suralar», «3-sahifa», «2-juz», «12-hadis» …
     */
    fun previousLabel(): String {
        val entry = controller.previousBackStackEntry ?: return Routes.sections.getValue(Routes.HOME)
        val route = entry.destination.route
        val arg = { name: String -> entry.arguments?.getString(name) }
        Routes.sections[route]?.let { return it }
        return when (route) {
            Routes.PAGE, Routes.SURAH_PAGE, Routes.JUZ_PAGE -> "${arg("number")}-sahifa"
            Routes.JUZ -> "${arg("number")}-juz"
            Routes.JUZ_PAGES -> "${arg("number")}-juz sahifalari"
            Routes.SURAH_PAGES -> "${arg("number")}-sura sahifalari"
            Routes.SURAH -> "${arg("number")}-sura"
            Routes.HADITH -> "${arg("number")}-hadis"
            Routes.BOOK, Routes.BOOK_SECTION -> "Hadislar"
            Routes.LIBRARY_BOOK, Routes.LIBRARY_ITEM, Routes.LIBRARY_TEXT -> "Kitoblar"
            else -> "Orqaga"
        }
    }

    companion object {
        /** BackLink(UP, …) — oldingi sahifaga qaytadi (orqaga tugmasi kabi) */
        const val UP = "__up__"
    }

    /** Yon paneldagi bo'lim: bosh sahifagacha qaytib, bo'limni ochadi */
    fun section(route: String) {
        if (route == Routes.HOME) {
            if (!controller.popBackStack(Routes.HOME, inclusive = false)) open(Routes.HOME)
            return
        }
        controller.navigate(route) {
            launchSingleTop = true
            popUpTo(Routes.HOME) { inclusive = false }
        }
    }
}

val LocalNav = staticCompositionLocalOf<NavActions> { error("NavActions berilmagan") }

/** Sahifaning navbardagi «‹ Orqaga» havolasi */
data class BackTarget(val route: String, val label: String)

@Stable
class BackLinkState {
    var current by mutableStateOf<BackTarget?>(null)
        internal set
}

val LocalBackLink = staticCompositionLocalOf { BackLinkState() }

/**
 * Sahifa ichida hech narsa chizmaydi — havola navbarda ko'rinadi, navbar esa doim tepada
 * turadi: qancha pastga tushilsa ham orqaga qaytish bir bosish.
 */
@Composable
fun BackLink(route: String, label: String) {
    val state = LocalBackLink.current
    DisposableEffect(route, label) {
        val target = BackTarget(route, label)
        state.current = target
        onDispose { if (state.current == target) state.current = null }
    }
}
