package com.example.kotlin_holy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.kotlin_holy.ui.HolyApplicationScreen
import com.example.kotlin_holy.ui.decoy.DecoyBrowserScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            /* Ilova ochilganda avval soxta brauzer ko'rinishi chiqadi — omniboxga
               3 ta probel + Enter kiritilgandagina haqiqiy ilova ochiladi */
            var unlocked by rememberSaveable { mutableStateOf(false) }
            if (unlocked) {
                HolyApplicationScreen()
            } else {
                DecoyBrowserScreen(onUnlock = { unlocked = true })
            }
        }
    }
}
