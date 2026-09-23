package com.example.kotlin_holy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.kotlin_holy.ui.components.HolyCard
import com.example.kotlin_holy.ui.navigation.HolyNavHost
import com.example.kotlin_holy.ui.navigation.Routes
import com.example.kotlin_holy.ui.theme.HolyTheme
import com.example.kotlin_holy.ui.theme.LocalHolyColors

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.SURAHS, "Suralar", Icons.Filled.MenuBook),
    Tab(Routes.PAGES, "Sahifalar", Icons.Outlined.Article),
    Tab(Routes.JUZ_LIST, "Juzlar", Icons.Filled.AutoStories),
    Tab(Routes.WORDS, "Soʻzlar", Icons.Filled.Translate),
    Tab(Routes.SETTINGS, "Sozlama", Icons.Filled.Settings),
)

/** Ilovaning ildizi: mavzu, pastki menyu va navigatsiya shu yerda */
@Composable
fun HolyApplicationScreen(viewModel: RootViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    HolyTheme(themeMode = settings.themeMode) {
        val colors = LocalHolyColors.current
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val destination = backStack?.destination

        Scaffold(
            containerColor = colors.bg,
            bottomBar = {
                NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                    tabs.forEach { tab ->
                        val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(tab.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                unselectedIconColor = colors.inkFaint,
                                unselectedTextColor = colors.inkFaint,
                                indicatorColor = colors.accentSoft,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(padding),
            ) {
                val lastReadRoute = settings.lastReadRoute
                val lastReadLabel = settings.lastReadLabel
                if (lastReadRoute != null && lastReadLabel != null) {
                    ContinueReadingBar(
                        label = lastReadLabel,
                        onClick = {
                            navController.navigate(lastReadRoute) { launchSingleTop = true }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Box(Modifier.weight(1f)) {
                    HolyNavHost(navController = navController, settings = settings)
                }
            }
        }
    }
}

/** Foydalanuvchi oxirgi oʻqigan joyiga bir bosishda qaytadigan panel */
@Composable
private fun ContinueReadingBar(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = HolyTheme.colors
    HolyCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Bookmark,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = "Davom etish:",
                color = colors.inkFaint,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = label,
                color = colors.ink,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = colors.inkFaint,
            )
        }
    }
}
