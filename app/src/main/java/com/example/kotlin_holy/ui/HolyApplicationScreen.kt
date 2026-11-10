package com.example.kotlin_holy.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kotlin_holy.ui.components.Divider
import com.example.kotlin_holy.ui.navigation.BackLinkState
import com.example.kotlin_holy.ui.navigation.HolyNavHost
import com.example.kotlin_holy.ui.navigation.LocalBackLink
import com.example.kotlin_holy.ui.navigation.LocalNav
import com.example.kotlin_holy.ui.navigation.NavActions
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.shell.AppHeader
import com.example.kotlin_holy.ui.shell.LocalShell
import com.example.kotlin_holy.ui.shell.QuranNav
import com.example.kotlin_holy.ui.shell.RouteInfo
import com.example.kotlin_holy.ui.shell.ShellInfo
import com.example.kotlin_holy.ui.shell.Sidebar
import com.example.kotlin_holy.ui.theme.AppearanceBackdrop
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.LocalTextScales
import com.example.kotlin_holy.ui.theme.TextScales
import kotlinx.coroutines.launch

/**
 * Ilovaning ildizi — veb nusxadagi Layout: tepada navbar, chapda yon panel (tor ekranda
 * menyu tugmasi bilan ochiladigan), orqada mavzu naqshi, o'rtada sahifalar.
 */
@Composable
fun HolyApplicationScreen(viewModel: RootViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val surahs by viewModel.surahs.collectAsStateWithLifecycle()
    val xatm by viewModel.xatm.collectAsStateWithLifecycle()
    val todayReading by viewModel.todayReading.collectAsStateWithLifecycle()

    /* Sozlamalar o'qilguncha hech narsa chizilmaydi — mavzu bir lahza almashib ko'rinmasin */
    val current = settings ?: run {
        Box(Modifier.fillMaxSize().background(Color(0xFF0F1012)))
        return
    }

    HolyTheme(themeMode = current.themeMode, appearance = current.appearance) {
        val colors = HolyTheme.colors

        /* Tizim panellaridagi belgilar ilova mavzusiga mos: qorong'i fonda — oq */
        val view = LocalView.current
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !colors.isDark
                isAppearanceLightNavigationBars = !colors.isDark
            }
        }

        val navController = rememberNavController()
        val nav = remember(navController) { NavActions(navController) }
        val backLink = remember { BackLinkState() }
        val entry by navController.currentBackStackEntryAsState()
        val route = remember(entry) {
            val destination = entry?.destination
            RouteInfo(
                pattern = destination?.route,
                args = destination?.arguments?.keys?.mapNotNull { key ->
                    entry?.arguments?.getString(key)?.let { key to it }
                }?.toMap().orEmpty(),
            )
        }

        ReadingTimeEffect(viewModel, route.pattern in Routes.readingRoutes)

        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(colors.bg)
                .pointerInput(Unit) {
                    /* O'qish vaqti: bosish va varaqlash — harakat (hodisalar iste'mol qilinmaydi) */
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            viewModel.readingTracker.activity()
                        }
                    }
                },
        ) {
            val wide = maxWidth >= 1024.dp
            val shell = ShellInfo(surahs = surahs, route = route, wide = maxWidth >= 640.dp)
            val drawerState = rememberDrawerState(DrawerValue.Closed)
            val scope = rememberCoroutineScope()

            LaunchedEffect(route.filled) { if (drawerState.isOpen) drawerState.close() }

            AppearanceBackdrop(current.appearance, colors, Modifier.fillMaxSize())

            CompositionLocalProvider(
                LocalNav provides nav,
                LocalBackLink provides backLink,
                LocalShell provides shell,
                LocalTextScales provides TextScales(current.arabicScale, current.translationScale),
            ) {
                val body: @Composable () -> Unit = {
                    Column(Modifier.fillMaxSize()) {
                        AppHeader(
                            showMenu = !wide,
                            onMenu = { scope.launch { drawerState.open() } },
                            onLogo = { nav.section(Routes.HOME) },
                            back = backLink.current,
                            onBack = nav::back,
                            lastReadLabel = current.lastReadLabel,
                            showContinue = current.lastReadRoute != null && current.lastReadRoute != route.filled,
                            onContinue = { current.lastReadRoute?.let(nav::open) },
                            themeMode = current.themeMode,
                            onThemeMode = viewModel::setThemeMode,
                            settingsActive = route.pattern == Routes.SETTINGS,
                            onSettings = { nav.open(Routes.settings()) },
                            xatm = xatm,
                            todayReading = todayReading,
                            onXatm = { nav.open(Routes.settings("xatm")) },
                            onReading = { nav.open(Routes.settings("reading")) },
                            quranNav = { QuranNav() },
                        )
                        Row(Modifier.fillMaxSize()) {
                            if (wide) {
                                Sidebar(
                                    currentRoute = route.pattern,
                                    onNavigate = nav::section,
                                    modifier = Modifier.width(240.dp),
                                )
                                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                            }
                            HolyNavHost(navController, Modifier.fillMaxSize())
                        }
                    }
                }

                if (wide) {
                    body()
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        scrimColor = Color.Black.copy(alpha = 0.5f),
                        drawerContent = {
                            ModalDrawerSheet(
                                modifier = Modifier.width(256.dp),
                                drawerShape = RectangleShape,
                                drawerContainerColor = colors.surface,
                                drawerTonalElevation = 0.dp,
                            ) {
                                Row(Modifier.fillMaxSize()) {
                                    Sidebar(
                                        currentRoute = route.pattern,
                                        onNavigate = { target ->
                                            scope.launch { drawerState.close() }
                                            nav.section(target)
                                        },
                                        modifier = Modifier.weight(1f).statusBarsPadding(),
                                    )
                                    Box(Modifier.width(1.dp).fillMaxHeight().background(colors.line))
                                }
                            }
                        },
                        content = body,
                    )
                }
            }
        }
    }
}

/** O'qish sahifasida va ilova ko'rinib turganda o'qish vaqti hisoblanadi */
@Composable
private fun ReadingTimeEffect(viewModel: RootViewModel, reading: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(reading, lifecycle) {
        val tracker = viewModel.readingTracker
        if (!reading) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> tracker.start()
                Lifecycle.Event.ON_STOP -> tracker.stop()
                else -> Unit
            }
        }
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) tracker.start()
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            tracker.stop()
        }
    }
}
