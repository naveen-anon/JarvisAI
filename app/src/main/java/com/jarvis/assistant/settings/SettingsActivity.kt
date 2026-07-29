package com.jarvis.assistant.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.util.PersistentMemory
import com.jarvis.assistant.util.SettingsManager
import com.jarvis.assistant.voice.TextToSpeechHelper
import com.jarvis.assistant.voice.VoiceAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Everything here previously existed only as voice commands ("change voice to male",
 * "remember X") with zero visible confirmation that anything actually happened. This
 * screen makes the same settings visible and directly editable, and — combined with the
 * TextToSpeechHelper fix — actually audible when you change voice type.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    private lateinit var memory: PersistentMemory
    private lateinit var tts: TextToSpeechHelper
    private lateinit var voiceAuth: VoiceAuthManager
    private lateinit var voiceTypeLabel: TextView
    private lateinit var noteText: TextView
    private lateinit var voiceAuthStatus: TextView
    private val scope = CoroutineScope(Dispatchers.Main)

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) runEnrollment() else toast("Microphone permission is needed to enroll your voice.") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsManager(this)
        memory = PersistentMemory(this)
        tts = TextToSpeechHelper(this)
        voiceAuth = VoiceAuthManager(this)
        setContentView(buildUi())
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050A0F"))
            setPadding(48, 64, 48, 64)
        }

        fun sectionTitle(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#00D4FF"))
            textSize = 16f
            setPadding(0, 40, 0, 12)
        }
        fun bodyText(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#8FC7D6"))
            textSize = 13f
        }

        root.addView(TextView(this).apply {
            text = "Jarvis Settings"
            setTextColor(Color.WHITE)
            textSize = 22f
        })

        root.addView(Button(this).apply {
            text = "📊 Usage Stats"
            setOnClickListener {
                startActivity(android.content.Intent(this@SettingsActivity, StatsActivity::class.java))
            }
        })

        // --- Voice type ---
        root.addView(sectionTitle("Voice Type"))
        voiceTypeLabel = bodyText("Current: ${settings.getVoiceType()}")
        root.addView(voiceTypeLabel)

        val voiceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("male", "female", "robot").forEach { type ->
            voiceRow.addView(Button(this).apply {
                text = type.replaceFirstChar { it.uppercase() }
                setOnClickListener {
                    settings.setVoiceType(type)
                    voiceTypeLabel.text = "Current: $type"
                    tts.speak("This is what my $type voice sounds like.")
                }
            })
        }
        root.addView(voiceRow)

        // --- Voice speed ---
        root.addView(sectionTitle("Voice Speed"))
        val speedLabel = bodyText("${settings.getVoiceSpeed()}x")
        root.addView(speedLabel)
        root.addView(SeekBar(this).apply {
            max = 20 // maps to 0.3x .. 2.3x
            progress = ((settings.getVoiceSpeed() - 0.3f) * 10).toInt().coerceIn(0, 20)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                    val speed = 0.3f + value / 10f
                    settings.setVoiceSpeed(speed)
                    speedLabel.text = "${"%.1f".format(speed)}x"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {
                    tts.speak("Voice speed test.")
                }
            })
        })

        // --- User name ---
        root.addView(sectionTitle("Your Name"))
        val nameInput = EditText(this).apply {
            setText(settings.getUserName())
            setTextColor(Color.WHITE)
        }
        root.addView(nameInput)
        root.addView(Button(this).apply {
            text = "Save Name"
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isNotBlank()) settings.setUserName(name)
            }
        })

        // --- Memory ---
        root.addView(sectionTitle("Remembered Note"))
        noteText = bodyText(memory.recall("last_note") ?: "(nothing remembered yet)")
        root.addView(noteText)
        root.addView(Button(this).apply {
            text = "Clear Note"
            setOnClickListener {
                memory.forget("last_note")
                noteText.text = "(nothing remembered yet)"
            }
        })

        // --- Voice Authentication ---
        root.addView(sectionTitle("Voice Authentication"))
        root.addView(bodyText(
            "Approximate on-device voice matching — not a bank-grade biometric, but good " +
            "enough to reject an obviously different voice. Checked once per app session, " +
            "not on every command."
        ))
        voiceAuthStatus = bodyText(voiceAuthStatusText())
        root.addView(voiceAuthStatus)

        root.addView(Button(this).apply {
            text = "🎙️ Enroll My Voice"
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(this@SettingsActivity, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    runEnrollment()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        })

        val voiceAuthToggle = Button(this).apply {
            text = if (voiceAuth.isEnabled()) "Disable Voice Lock" else "Enable Voice Lock"
            setOnClickListener {
                if (!voiceAuth.isEnrolled()) {
                    toast("Enroll your voice first.")
                    return@setOnClickListener
                }
                val nowEnabled = !voiceAuth.isEnabled()
                voiceAuth.setEnabled(nowEnabled)
                text = if (nowEnabled) "Disable Voice Lock" else "Enable Voice Lock"
                voiceAuthStatus.text = voiceAuthStatusText()
            }
        }
        root.addView(voiceAuthToggle)

        root.addView(Button(this).apply {
            text = "Reset Voice Enrollment"
            setOnClickListener {
                voiceAuth.resetEnrollment()
                voiceAuthStatus.text = voiceAuthStatusText()
                voiceAuthToggle.text = "Enable Voice Lock"
                toast("Voice enrollment cleared.")
            }
        })

        // --- Lock Screen ---
        root.addView(sectionTitle("Lock Screen"))
        root.addView(bodyText(
            "Android no longer allows regular apps to set or change your device's lock " +
            "screen PIN/pattern directly (a security restriction since Android 8) — only " +
            "this shortcut into system settings is possible. For an in-app lock instead, " +
            "use \"lock [app name]\" by voice, which is Jarvis's own PIN-gated app lock."
        ))
        root.addView(Button(this).apply {
            text = "Open Lock Screen Settings"
            setOnClickListener {
                try {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } catch (e: Exception) {
                    toast("Couldn't open security settings on this device.")
                }
            }
        })

        root.addView(Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun voiceAuthStatusText(): String = when {
        !voiceAuth.isEnrolled() -> "Not enrolled yet."
        voiceAuth.isEnabled() -> "Enrolled — voice lock is ON."
        else -> "Enrolled — voice lock is OFF."
    }

    /** Captures 3 short samples with prompts between each, then stores the averaged voiceprint. */
    private fun runEnrollment() {
        toast("Enrolling — say a short phrase 3 times when prompted.")
        scope.launch {
            val samples = mutableListOf<ShortArray>()
            repeat(3) { i ->
                toast("Sample ${i + 1} of 3 — speak now…")
                val sample = withContext(Dispatchers.IO) { voiceAuth.captureSample(1800) }
                if (sample != null) samples.add(sample)
            }
            if (samples.size < 2) {
                toast("Didn't capture enough audio — try again somewhere quieter.")
                return@launch
            }
            voiceAuth.enrollFromSamples(samples)
            voiceAuth.setEnabled(true)
            voiceAuthStatus.text = voiceAuthStatusText()
            toast("Voice enrolled and voice lock enabled.")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
