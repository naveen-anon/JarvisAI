package com.jarvis.assistant.ui.briefing

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jarvis.assistant.ui.compose.BriefingScreen
import com.jarvis.assistant.voice.TextToSpeechHelper

class BriefingActivity : ComponentActivity() {
    private var tts: TextToSpeechHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        tts = TextToSpeechHelper(this)
        setContent {
            BriefingScreen(
                onBack = { finish() },
                onSpeak = { text -> tts?.speak(text) }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}
