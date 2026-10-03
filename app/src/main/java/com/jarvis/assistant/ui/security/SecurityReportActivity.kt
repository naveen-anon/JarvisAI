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
    private var lastSpoken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_report)
        tts = TextToSpeechHelper(this)

        findViewById<TextView>(R.id.btnSecBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSecSpeak).setOnClickListener {
            if (lastSpoken.isNotBlank()) tts?.speak(lastSpoken)
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
                try {
                    SecurityAdvisor(this@SecurityReportActivity).assess()
                } catch (_: Exception) {
                    null
                }
            }
            if (report == null) {
                title.text = "Security report"
                body.text = "Assessment unavailable."
                score.text = "—"
                lastSpoken = ""
                return@launch
            }
            title.text = "Grade ${report.grade} · Score ${report.score}"
            score.text = "${report.score}"
            body.text = formatBody(report)
            lastSpoken = report.spoken
        }
    }

    private fun formatBody(report: SecurityAdvisor.Report): String {
        val posture = when {
            report.score >= 85 -> "strong"
            report.score >= 70 -> "fair"
            else -> "needs attention"
        }
        val sb = StringBuilder()
        sb.append("Security posture is ").append(posture).append(", sir.\n\n")
        val problems = report.findings.filter { it.severity != SecurityAdvisor.Severity.OK }
        if (problems.isEmpty()) {
            sb.append("No priority issues. All checked controls look healthy.")
        } else {
            sb.append("Priority findings:\n")
            problems.forEachIndexed { i, f ->
                sb.append(i + 1).append(". ").append(f.title).append('\n')
                if (f.detail.isNotBlank()) sb.append("   ").append(f.detail).append('\n')
                val act = f.action
                if (!act.isNullOrBlank()) sb.append("   → ").append(act).append('\n')
                sb.append('\n')
            }
        }
        val okCount = report.findings.count { it.severity == SecurityAdvisor.Severity.OK }
        sb.append(okCount).append(" controls look healthy. Defensive assessment only.")
        return sb.toString().trim()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}
