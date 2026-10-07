package com.jarvis.assistant.ui.clock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jarvis.assistant.ui.compose.ClockScreen

class ClockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val mode = intent.getStringExtra(EXTRA_MODE)?.lowercase()
        val tab = when (mode) {
            "timer" -> 1
            "stopwatch" -> 2
            else -> 0
        }
        setContent {
            ClockScreen(onBack = { finish() }, initialTab = tab)
        }
    }

    companion object {
        const val EXTRA_MODE = "clock_mode"
        const val EXTRA_SECONDS = "timer_seconds"
        const val EXTRA_AUTO_START = "auto_start"
    }
}
