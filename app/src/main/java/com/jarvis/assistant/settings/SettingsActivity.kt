package com.jarvis.assistant.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.util.PersistentMemory
import com.jarvis.assistant.util.SettingsManager
import com.jarvis.assistant.voice.TextToSpeechHelper
import com.jarvis.assistant.voice.VoiceAuthManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    private lateinit var memory: PersistentMemory
    private lateinit var tts: TextToSpeechHelper
    private lateinit var voiceAuth: VoiceAuthManager
    private lateinit var voiceTypeLabel: TextView
    private lateinit var noteText: TextView
    private lateinit var voiceAuthStatus: TextView
    private val voiceChipRefs = mutableMapOf<String, LinearLayout>()
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
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(bg)
            addView(com.jarvis.assistant.ui.CornerFrameView(this@SettingsActivity))
            addView(buildUi())
        })
    }

    private val cyan = Color.parseColor("#00E5FF")
    private val cyanDim = Color.parseColor("#0B7A94")
    private val hudText = Color.parseColor("#B8D4E0")
    private val hudTextDim = Color.parseColor("#5A8A9A")
    private val bg = Color.parseColor("#03080E")

    private fun hudButton(label: String, filled: Boolean = false, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setTextColor(if (filled) Color.parseColor("#03080E") else cyan)
        background = ContextCompat.getDrawable(
            this@SettingsActivity,
            if (filled) R.drawable.hud_button_filled else R.drawable.hud_button_bg
        )
        setPadding(32, 28, 32, 28)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = 16
        layoutParams = lp
        setOnClickListener { onClick() }
    }

    // A tappable row with a circular icon, title, subtitle, and a chevron — matches "USAGE STATS" / "SEND FEEDBACK" cards
    private fun linkRow(icon: String, title: String, subtitle: String, onClick: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        isClickable = true
        isFocusable = true
        background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.hud_button_bg)
        setPadding(28, 24, 28, 24)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 14
        layoutParams = lp

        addView(TextView(this@SettingsActivity).apply {
            text = icon
            textSize = 20f
            gravity = Gravity.CENTER
            val size = 88
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = 24 }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setStroke(2, cyanDim)
                setColor(Color.parseColor("#0B1520"))
            }
        })

        addView(LinearLayout(this@SettingsActivity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@SettingsActivity).apply {
                text = title
                setTextColor(cyan)
                textSize = 15f
                typeface = Typeface.MONOSPACE
            })
            addView(TextView(this@SettingsActivity).apply {
                text = subtitle
                setTextColor(hudTextDim)
                textSize = 12f
            })
        })

        addView(TextView(this@SettingsActivity).apply {
            text = "\u203A"
            setTextColor(cyanDim)
            textSize = 22f
        })

        setOnClickListener { onClick() }
    }

    private fun sectionCard(builder: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.hud_card_bg)
        setPadding(36, 32, 36, 32)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = 32
        layoutParams = lp
        builder()
    }

    private fun sectionTitle(text: String, icon: String = "") = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(this@SettingsActivity).apply {
            layoutParams = LinearLayout.LayoutParams(6, 36)
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.hud_accent_bar)
        })
        addView(TextView(this@SettingsActivity).apply {
            val prefix = if (icon.isNotEmpty()) "  $icon " else "  "
            this.text = "$prefix${text.uppercase()}"
            setTextColor(cyan)
            textSize = 14f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
        })
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 16
        layoutParams = lp
    }

    private fun bodyText(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(hudText)
        textSize = 13f
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = 12
        layoutParams = lp
    }

    private fun hudEditText(initial: String) = EditText(this).apply {
        setText(initial)
        setTextColor(Color.WHITE)
        setHintTextColor(hudTextDim)
        background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.hud_button_bg)
        setPadding(28, 20, 28, 20)
    }

    // Voice type selector card — icon + label, highlighted border when selected
    private fun voiceTypeChip(type: String, icon: String, selected: Boolean, onClick: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        isClickable = true
        isFocusable = true
        setPadding(16, 32, 16, 32)
        background = GradientDrawable().apply {
            cornerRadius = 28f
            setColor(if (selected) Color.parseColor("#1F00E5FF") else Color.TRANSPARENT)
            setStroke(3, if (selected) cyan else Color.parseColor("#16303D"))
        }
        val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        lp.marginEnd = 12
        layoutParams = lp

        addView(TextView(this@SettingsActivity).apply {
            text = icon
            textSize = 26f
            gravity = Gravity.CENTER
        })
        addView(TextView(this@SettingsActivity).apply {
            text = type.uppercase()
            setTextColor(if (selected) cyan else hudTextDim)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            val tlp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            tlp.topMargin = 8
            layoutParams = tlp
        })

        setOnClickListener { onClick() }
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 80)
        }

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@SettingsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(8, 48)
                background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.hud_accent_bar)
            })
            addView(TextView(this@SettingsActivity).apply {
                text = "  J.A.R.V.I.S — SETTINGS"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.MONOSPACE
            })
        })

        // Usage stats + feedback as icon link-rows
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 28
            layoutParams = lp

            addView(linkRow("\uD83D\uDCCA", "USAGE STATS", "View system usage and activity") {
                startActivity(Intent(this@SettingsActivity, StatsActivity::class.java))
            })
            addView(linkRow("\uD83D\uDCAC", "SEND FEEDBACK", "Help improve J.A.R.V.I.S") {
                startActivity(Intent(this@SettingsActivity, FeedbackActivity::class.java))
            })
        })

        root.addView(sectionCard {
            addView(sectionTitle("Voice Type"))
            voiceTypeLabel = bodyText("Current: ${settings.getVoiceType()}")
            addView(voiceTypeLabel)

            val voiceRow = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            val icons = mapOf("male" to "\uD83D\uDC68", "female" to "\uD83D\uDC69", "robot" to "\uD83E\uDD16")
            fun refreshChips(current: String) {
                voiceRow.removeAllViews()
                listOf("male", "female", "robot").forEach { type ->
                    voiceRow.addView(voiceTypeChip(type, icons[type] ?: "\u25CF", type == current) {
                        settings.setVoiceType(type)
                        voiceTypeLabel.text = "Current: $type"
                        tts.speak("This is what my $type voice sounds like.")
                        refreshChips(type)
                    })
                }
            }
            refreshChips(settings.getVoiceType())
            addView(voiceRow)
        })

        root.addView(sectionCard {
            addView(sectionTitle("Voice Speed"))
            val speedLabel = bodyText("${settings.getVoiceSpeed()}x")
            addView(speedLabel)
            addView(SeekBar(this@SettingsActivity).apply {
                max = 20
                progress = ((settings.getVoiceSpeed() - 0.3f) * 10).toInt().coerceIn(0, 20)
                progressTintList = android.content.res.ColorStateList.valueOf(cyan)
                thumbTintList = android.content.res.ColorStateList.valueOf(cyan)
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(sb: SeekBar?, value: Int, fromUser: Boolean) {
                        val speed = 0.3f + value / 10f
                        settings.setVoiceSpeed(speed)
                        speedLabel.text = "${"%.1f".format(speed)}x"
                    }
                    override fun onStartTrackingTouch(sb: SeekBar?) {}
                    override fun onStopTrackingTouch(sb: SeekBar?) { tts.speak("Voice speed test.") }
                })
            })
        })

        root.addView(sectionCard {
            addView(sectionTitle("Your Name"))
            val nameInput = hudEditText(settings.getUserName())
            addView(nameInput)
            addView(hudButton("SAVE NAME") {
                val name = nameInput.text.toString().trim()
                if (name.isNotBlank()) { settings.setUserName(name); toast("Saved.") }
            })
        })

        root.addView(sectionCard {
            addView(sectionTitle("Remembered Note"))
            noteText = bodyText(memory.recall("last_note") ?: "(nothing remembered yet)")
            addView(noteText)
            addView(hudButton("CLEAR NOTE") {
                memory.forget("last_note")
                noteText.text = "(nothing remembered yet)"
            })
        })

        root.addView(sectionCard {
            addView(sectionTitle("Voice Authentication", "\uD83C\uDF99\uFE0F"))
            addView(bodyText(
                "Approximate on-device voice matching — not a bank-grade biometric, but good " +
                "enough to reject an obviously different voice. Checked once per app session, " +
                "not on every command."
            ))
            voiceAuthStatus = bodyText(voiceAuthStatusText())
            addView(voiceAuthStatus)

            addView(hudButton("\uD83C\uDF99\uFE0F  ENROLL MY VOICE", filled = true) {
                if (ContextCompat.checkSelfPermission(this@SettingsActivity, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    runEnrollment()
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            })

            lateinit var voiceAuthToggle: Button
            voiceAuthToggle = hudButton(
                if (voiceAuth.isEnabled()) "DISABLE VOICE LOCK" else "ENABLE VOICE LOCK"
            ) {
                if (!voiceAuth.isEnrolled()) {
                    toast("Enroll your voice first.")
                } else {
                    val nowEnabled = !voiceAuth.isEnabled()
                    voiceAuth.setEnabled(nowEnabled)
                    voiceAuthStatus.text = voiceAuthStatusText()
                }
            }
            addView(voiceAuthToggle)

            addView(hudButton("RESET VOICE ENROLLMENT") {
                voiceAuth.resetEnrollment()
                voiceAuthStatus.text = voiceAuthStatusText()
                voiceAuthToggle.text = "ENABLE VOICE LOCK"
                toast("Voice enrollment cleared.")
            })
        })

        root.addView(sectionCard {
            addView(sectionTitle("Lock Screen", "\uD83D\uDD12"))
            addView(bodyText(
                "Android no longer allows regular apps to set or change your device's lock " +
                "screen PIN/pattern directly (a security restriction since Android 8) — only " +
                "this shortcut into system settings is possible. For an in-app lock instead, " +
                "use \"lock [app name]\" by voice, which is Jarvis's own PIN-gated app lock."
            ))
            addView(hudButton("OPEN LOCK SCREEN SETTINGS") {
                try {
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                } catch (e: Exception) {
                    toast("Couldn't open security settings on this device.")
                }
            })
        })

        root.addView(hudButton("\u2190  CLOSE") { finish() }.apply {
            val lp = layoutParams as LinearLayout.LayoutParams
            lp.topMargin = 48
            layoutParams = lp
        })

        return ScrollView(this).apply {
            addView(root)
        }
    }

    private fun voiceAuthStatusText(): String = when {
        !voiceAuth.isEnrolled() -> "Not enrolled yet."
        voiceAuth.isEnabled() -> "Enrolled — voice lock is ON."
        else -> "Enrolled — voice lock is OFF."
    }

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
