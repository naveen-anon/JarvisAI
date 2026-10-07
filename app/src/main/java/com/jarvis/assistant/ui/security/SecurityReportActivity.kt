package com.jarvis.assistant.ui.security

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
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
        findViewById<TextView>(R.id.txtSecSummary).text = "Scanning posture…"
        findViewById<TextView>(R.id.txtSecScore).text = "—"
        findViewById<TextView>(R.id.txtSecGrade).text = "Grade —"
        findViewById<LinearLayout>(R.id.secFindingsHost).removeAllViews()

        CoroutineScope(Dispatchers.Main).launch {
            val report = withContext(Dispatchers.IO) {
                try {
                    SecurityAdvisor(this@SecurityReportActivity).assess()
                } catch (_: Exception) {
                    null
                }
            }
            if (report == null) {
                findViewById<TextView>(R.id.txtSecSummary).text = "Assessment unavailable."
                return@launch
            }
            bindReport(report)
        }
    }

    private fun bindReport(report: SecurityAdvisor.Report) {
        findViewById<TextView>(R.id.txtSecScore).text = "${report.score}"
        findViewById<TextView>(R.id.txtSecGrade).text = "Grade ${report.grade}"
        findViewById<TextView>(R.id.txtSecTitle).text = "Score ${report.score}"
        val posture = when {
            report.score >= 85 -> "strong"
            report.score >= 70 -> "fair"
            else -> "needs attention"
        }
        findViewById<TextView>(R.id.txtSecSummary).text =
            "Security posture is $posture, sir."

        val crit = report.findings.count { it.severity == SecurityAdvisor.Severity.CRITICAL }
        val high = report.findings.count { it.severity == SecurityAdvisor.Severity.HIGH }
        val med = report.findings.count { it.severity == SecurityAdvisor.Severity.MEDIUM }
        val low = report.findings.count { it.severity == SecurityAdvisor.Severity.LOW }

        findViewById<TextView>(R.id.txtRiskCritical).text = "● Critical  $crit"
        findViewById<TextView>(R.id.txtRiskHigh).text = "● High  $high"
        findViewById<TextView>(R.id.txtRiskMedium).text = "● Medium  $med"
        findViewById<TextView>(R.id.txtRiskLow).text = "● Low  $low"

        val issues = report.findings.filter {
            it.severity != SecurityAdvisor.Severity.OK
        }
        findViewById<TextView>(R.id.txtIssueCount).text =
            "${issues.size} ISSUE${if (issues.size == 1) "" else "S"} FOUND"

        val host = findViewById<LinearLayout>(R.id.secFindingsHost)
        host.removeAllViews()
        if (issues.isEmpty()) {
            host.addView(TextView(this).apply {
                text = "No priority issues — controls look healthy."
                setTextColor(Color.parseColor("#5A8A99"))
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setPadding(8, 16, 8, 16)
            })
        } else {
            issues.forEach { f -> host.addView(findingCard(f)) }
        }

        lastSpoken = report.spoken
        findViewById<TextView>(R.id.txtSecBody).text = report.spoken
    }

    private fun findingCard(f: SecurityAdvisor.Finding): LinearLayout {
        val (badge, color) = when (f.severity) {
            SecurityAdvisor.Severity.CRITICAL -> "CRITICAL" to "#FF6B6B"
            SecurityAdvisor.Severity.HIGH -> "HIGH RISK" to "#FFAA00"
            SecurityAdvisor.Severity.MEDIUM -> "MEDIUM RISK" to "#E8C547"
            SecurityAdvisor.Severity.LOW -> "LOW RISK" to "#00D9FF"
            else -> "OK" to "#5A8A99"
        }
        val border = Color.parseColor(color)
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 18, 20, 18)
            background = GradientDrawable().apply {
                cornerRadius = 20f
                setColor(0xCC0A1825.toInt())
                setStroke(2, border and 0x00FFFFFF or 0x99000000.toInt())
                // stroke with alpha
                setStroke(2, Color.argb(0x99, Color.red(border), Color.green(border), Color.blue(border)))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10 }
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(TextView(this).apply {
            text = f.title
            setTextColor(Color.parseColor("#E8FBFF"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        top.addView(TextView(this).apply {
            text = "⚠ $badge"
            setTextColor(border)
            textSize = 10f
            typeface = Typeface.MONOSPACE
            setTypeface(typeface, Typeface.BOLD)
            setPadding(12, 6, 12, 6)
            background = GradientDrawable().apply {
                cornerRadius = 12f
                setColor(Color.argb(0x33, Color.red(border), Color.green(border), Color.blue(border)))
                setStroke(1, border)
            }
        })
        card.addView(top)

        card.addView(TextView(this).apply {
            text = f.detail
            setTextColor(Color.parseColor("#8AB4C0"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, 8, 0, 0)
        })
        if (!f.action.isNullOrBlank()) {
            card.addView(TextView(this).apply {
                text = "→ ${f.action}"
                setTextColor(Color.parseColor("#5A8A99"))
                textSize = 11f
                typeface = Typeface.MONOSPACE
                setPadding(0, 6, 0, 0)
            })
        }

        val actionBtn = TextView(this).apply {
            text = if (f.severity == SecurityAdvisor.Severity.HIGH ||
                f.severity == SecurityAdvisor.Severity.CRITICAL
            ) "FIX  ›" else "REVIEW  ›"
            setTextColor(Color.parseColor("#00D9FF"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(20, 12, 20, 12)
            background = GradientDrawable().apply {
                cornerRadius = 14f
                setColor(0x330A1825)
                setStroke(1, 0x8800D9FF.toInt())
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 12
            layoutParams = lp
            setOnClickListener { openFix(f.id) }
        }
        card.addView(actionBtn)
        return card
    }

    private fun openFix(id: String) {
        try {
            val intent = when {
                id.contains("vpn", true) || id.contains("net", true) ->
                    Intent(Settings.ACTION_WIRELESS_SETTINGS)
                id.contains("notif", true) ->
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                id.contains("a11y", true) || id.contains("access", true) ->
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                id.contains("voice", true) ->
                    Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java)
                id.contains("timeout", true) || id.contains("lock", true) ->
                    Intent(Settings.ACTION_SECURITY_SETTINGS)
                id.contains("adb", true) ->
                    Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts?.shutdown() } catch (_: Exception) {}
    }
}
