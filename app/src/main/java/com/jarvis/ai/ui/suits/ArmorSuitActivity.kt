package com.jarvis.ai.ui.suits

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class ArmorSuitActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.parseColor("#020810")
        window.navigationBarColor = AndroidColor.parseColor("#020810")
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFF00E5FF),
                background = Color(0xFF020810),
                surface = Color(0xFF0D1117)
            )) {
                ArmorSuitWindow(onClose = { finish() })
            }
        }
    }
}
