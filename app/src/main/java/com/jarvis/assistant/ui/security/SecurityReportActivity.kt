package com.jarvis.assistant.ui.security

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.util.SecurityAdvisor
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SecurityReportActivity : AppCompatActivity() {
    private var tts: TextToSpeechHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_report)
        tts = TextToSpeechHelper(this)

        findViewById<TextView>(R.id.btnSecBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSecSpeak).setOnClickListener {
            val body = findViewById<TextView>(R.id.txtSecBody).text.toString()
            if (body.isNotBlank()) tts?.speak(body)
        }
        findViewById<TextView>(R.id.btnSecRefresh).setOnClickListener { load() }
        load()
    }

    private fun load() {
        val title = findViewById<TextView>(R.id.txtSecTitle)
        val body = findViewById<TextView>(R.id.txtSecBody)
        val score = findViewById<TextView>(R.id.txtSecScore)
        title.text = "Scanning posture…"
        body.text = "Running local defensive checks…"
        CoroutineScope(Dispatchers.Main).launch {
            val report = withContext(Dispatchers.IO) {
                try { SecurityAdvisor(this@SecurityReportActivity).assess() }
                catch (e: Exception) { null }
            }
            if (report == null) {
                title.text = "Security report"
                body.text = "Assessment unavailable."
                score.text = "—"
                return@launch
            }
            title.text = "Grade ${report.grade} · Score ${report.score}"
            score.text = "${report.score}"
            body.text = report.spoken
            tts?.speak(report.spoken)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}
