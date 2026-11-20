package com.example.kotlin_holy.ui.decoy

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.kotlin_holy.R

private data class QuickLink(val label: String, @param:DrawableRes val icon: Int?, val url: String?)

private val quickLinks = listOf(
    QuickLink("Microsoft 365", R.drawable.ql_m365, "https://www.microsoft365.com/"),
    QuickLink("Booking.com", R.drawable.ql_booking, "https://www.booking.com/"),
    QuickLink("Kayak", R.drawable.ql_kayak, "https://www.kayak.com/"),
    QuickLink("eBay", R.drawable.ql_ebay, "https://www.ebay.com/"),
    QuickLink("AliExpress", R.drawable.ql_aliexpress, "https://www.aliexpress.com/"),
    QuickLink("Facebook", R.drawable.ql_facebook, "https://www.facebook.com/"),
    QuickLink("Outlook", R.drawable.ql_outlook, "https://outlook.live.com/"),
    QuickLink("Linkedin", R.drawable.ql_linkedin, "https://www.linkedin.com/"),
    QuickLink("Add shortcut", null, null),
)

private val backgrounds = listOf(
    R.drawable.bg1, R.drawable.bg2, R.drawable.bg3, R.drawable.bg4, R.drawable.bg5,
    R.drawable.bg6, R.drawable.bg7, R.drawable.bg8, R.drawable.bg9, R.drawable.bg10,
)

/* Yuqoridagi tugmalar va matn yorqin rasm ustida ham o'qilishi uchun */
private val topScrim = Brush.verticalGradient(
    0.0f to Color(0x73000000),
    0.5f to Color(0x26000000),
    1.0f to Color(0x00000000),
)

private val TileBg = Color(0xD9404040)

private val labelShadow =Shadow(color = Color(0xCC000000), offset = Offset(0f, 1f), blurRadius = 4f)

/**
 * Soxta "brauzer bosh sahifasi" — kuzatuvchi uchun oddiy brauzer kabi ko'rinadi.
 * Qidiruv satriga aynan 3 ta probel + Enter kiritilsa, haqiqiy ilova ochiladi;
 * boshqa har qanday matn qurilmadagi standart brauzerga yo'naltiriladi.
 */
@Composable
fun DecoyBrowserScreen(onUnlock: () -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    /* Har ochilganda 10 ta fon rasmidan biri tasodifiy tanlanadi */
    val background = rememberSaveable { backgrounds.random() }
    val labelSize = with(LocalDensity.current) { 11.dp.toSp() }

    /* Rasm ustida tizim panelidagi belgilar oq bo'lsin */
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    fun submit(value: String) {
        if (value == "   ") {
            onUnlock()
            return
        }
        val trimmed = value.trim()
        if (trimmed.isNotEmpty()) openInBrowser(context, trimmed)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF1C1A26))) {
        Image(
            painter = painterResource(background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxWidth().height(320.dp).background(topScrim))

        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Apps, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.Settings, contentDescription = null, tint = Color.White)
                }
                OutlinedButton(onClick = {}) {
                    Text("Sign in", color = Color.White)
                }
            }

            Spacer(Modifier.height(28.dp))

            Box(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF707070))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search the web", color = Color(0xFF6B6B6B), fontSize = 16.sp)
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(color = Color(0xFF1A1A1A), fontSize = 16.sp),
                            cursorBrush = SolidColor(Color.Black),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submit(query) }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(quickLinks) { link ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = link.url != null) {
                                link.url?.let { openInBrowser(context, it) }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(TileBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (link.icon != null) {
                                Image(
                                    painter = painterResource(link.icon),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)),
                                )
                            } else {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        /* Edge'dagidek uzun nomlar to'liq ko'rinadi; o'lcham tizim shrift
                           masshtabiga bog'liq emas — aks holda qo'shni yozuvlar ustma-ust tushadi */
                        Text(
                            link.label,
                            color = Color.White,
                            fontSize = labelSize,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            style = LocalTextStyle.current.copy(shadow = labelShadow),
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                        )
                    }
                }
            }
        }
    }
}

private val urlLikePattern = Regex("^[a-z][a-z0-9+.-]*://", RegexOption.IGNORE_CASE)

private fun openInBrowser(context: Context, input: String) {
    val looksLikeUrl = urlLikePattern.containsMatchIn(input) || (!input.contains(" ") && input.contains("."))
    val target = when {
        urlLikePattern.containsMatchIn(input) -> input
        looksLikeUrl -> "https://$input"
        else -> "https://www.google.com/search?q=" + Uri.encode(input)
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
    } catch (_: ActivityNotFoundException) {
        // Qurilmada brauzer topilmadi — jim o'tkazib yuboriladi
    }
}
