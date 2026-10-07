package com.jarvis.assistant.ui.security

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jarvis.assistant.ui.compose.SecurityScreen
import com.jarvis.assistant.voice.TextToSpeechHelper

class SecurityReportActivity : ComponentActivity() {
    private var tts: TextToSpeechHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeechHelper(this)
        setContent {
            SecurityScreen(
                onBack = { finish() },
                onSpeak = { tts?.speak(it) }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}
