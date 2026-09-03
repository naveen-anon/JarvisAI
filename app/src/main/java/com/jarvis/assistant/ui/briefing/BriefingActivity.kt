package com.jarvis.assistant.ui.briefing

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.util.BriefingHelper
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BriefingActivity : AppCompatActivity() {

    private lateinit var tts: TextToSpeechHelper
    private var lastText: String = ""
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_briefing)
        tts = TextToSpeechHelper(this)

        findViewById<TextView>(R.id.btnBriefingBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnGenerateBriefing).setOnClickListener { generate() }
        findViewById<TextView>(R.id.btnReadAloud).setOnClickListener {
            if (lastText.isNotBlank()) tts.speak(lastText)
            else generate(andSpeak = true)
        }
        generate()
    }

    private fun generate(andSpeak: Boolean = false) {
        val body = findViewById<TextView>(R.id.txtBriefingBody)
        body.text = "Generating briefing…"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    BriefingHelper(this@BriefingActivity).build(includeWeather = true)
                } catch (e: Exception) {
                    BriefingHelper.Briefing(
                        text = "Briefing unavailable: ${e.message}",
                        parts = emptyList()
                    )
                }
            }
            lastText = result.text
            findViewById<TextView>(R.id.txtBriefingTitle).text = result.parts.firstOrNull() ?: "Briefing"
            body.text = result.text
            val score = (60 + (result.parts.size * 8)).coerceAtMost(99)
            findViewById<TextView>(R.id.txtBriefingScore).text = "$score%"
            findViewById<TextView>(R.id.txtPendingTasks).text = "0"
            if (andSpeak) tts.speak(result.text)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts.shutdown() } catch (_: Exception) {}
    }
}
