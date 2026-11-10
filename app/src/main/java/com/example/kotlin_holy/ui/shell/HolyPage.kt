package com.example.kotlin_holy.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Kontent kengliklari — veb nusxadagi Layout: o'qish 56rem, bo'limlar 72rem, betlar ro'yxati 80rem */
object PageWidth {
    val Reading = 896.dp
    val Section = 1152.dp
    val Wide = 1280.dp
}

/**
 * Har bir sahifaning qobig'i (veb nusxadagi <main> ichi): px-4 py-8, tor ekranda boshida
 * «Qur'on bo'ylab o'tish» tugmasi, keng ekranda kontent markazda cheklangan kenglikda.
 */
@Composable
fun HolyPage(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    maxWidth: Dp = PageWidth.Reading,
    content: LazyListScope.() -> Unit,
) {
    val shell = LocalShell.current
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            state = state,
            modifier = modifier.fillMaxSize().widthIn(max = maxWidth + 48.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 32.dp, bottom = 32.dp + bottom),
        ) {
            if (!shell.wide) {
                item(key = "quran-nav") { QuranNav(Modifier.fillMaxWidth().padding(bottom = 24.dp)) }
            }
            content()
        }
    }
}
