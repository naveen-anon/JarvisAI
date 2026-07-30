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
import android.view.View
import android.view.ViewGroup
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

class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    private lateinit var memory: PersistentMemory
    private lateinit var tts: TextToSpeechHelper
    private lateinit var voiceAuth: VoiceAuthManager
    private lateinit var voiceTypeLabel: TextView
    private lateinit var noteText: TextView
    private lateinit var voiceAuthStatus: TextView
    private val scope = CoroutineScope(Dispatchers.Main)

    private val C_BG = Color.parseColor("#03080E")
    private val C_CARD = Color.parseColor("#0A1520")
    private val C_BORDER = Color.parseColor("#1A3A4A")
    private val C_CYAN = Color.parseColor("#00E5FF")
    private val C_CYAN_DIM = Color.parseColor("#0B7A94")
    private val C_TEXT = Color.parseColor("#B8D4E0")
    private val C_MUTED = Color.parseColor("#5A8A9A")
    private val C_WHITE = Color.parseColor("#F0FBFF")

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

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun cardBg() = GradientDrawable().apply {
        setColor(C_CARD)
        cornerRadius = dp(14).toFloat()
        setStroke(dp(1), C_BORDER)
    }

    private fun outlinedBtnBg() = GradientDrawable().apply {
        setColor(Color.parseColor("#122230"))
        cornerRadius = dp(10).toFloat()
        setStroke(dp(1), C_CYAN)
    }

    private fun filledBtnBg() = GradientDrawable().apply {
        setColor(C_CYAN_DIM)
        cornerRadius = dp(10).toFloat()
    }

    private fun styleButton(btn: Button, filled: Boolean = false) {
        btn.background = if (filled) filledBtnBg() else outlinedBtnBg()
        btn.setTextColor(if (filled) C_WHITE else C_CYAN)
        btn.isAllCaps = false
        btn.textSize = 13f
        btn.typeface = Typeface.MONOSPACE
        btn.setPadding(dp(16), dp(12), dp(16), dp(12))
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = dp(8)
        btn.layoutParams = lp
    }

    private fun sectionCard(title: String, block: LinearLayout.() -> Unit): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg()
            setPadding(dp(16), dp(16), dp(16), dp(16))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(14)
            layoutParams = lp
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(3), dp(14)).apply { marginEnd = dp(10) }
            setBackgroundColor(C_CYAN)
        })
        header.addView(TextView(this).apply {
            text = title
            setTextColor(C_CYAN)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
        })
        card.addView(header)
        card.block()
        return card
    }

    private fun body(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(C_TEXT)
        textSize = 13f
        typeface = Typeface.MONOSPACE
        setPadding(0, dp(8), 0, 0)
    }

    private fun muted(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(C_MUTED)
        textSize = 12f
        typeface = Typeface.MONOSPACE
        setPadding(0, dp(6), 0, 0)
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(C_BG)
            setPadding(dp(20), dp(48), dp(20), dp(40))
        }

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(View(this@SettingsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(22)).apply { marginEnd = dp(12) }
                setBackgroundColor(C_CYAN)
            })
            addView(TextView(this@SettingsActivity).apply {
                text = "SETTINGS"
                setTextColor(C_WHITE)
                textSize = 22f
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.1f
            })
        })
        root.addView(muted("Configure voice, identity & security").apply {
            setPadding(dp(16), dp(4), 0, dp(8))
        })

        root.addView(Button(this).apply {
            text = "📊  Usage Stats"
            styleButton(this, filled = true)
            setOnClickListener {
                startActivity(Intent(this@SettingsActivity, StatsActivity::class.java))
            }
        })

        root.addView(sectionCard("VOICE TYPE") {
            voiceTypeLabel = body("Current: ${settings.getVoiceType()}")
            addView(voiceTypeLabel)
            val voiceRow = LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }
            listOf("male", "female", "robot").forEach { type ->
                voiceRow.addView(Button(this@SettingsActivity).apply {
                    text = type.replaceFirstChar { it.uppercase() }
                    background = outlinedBtnBg()
                    setTextColor(C_CYAN)
                    isAllCaps = false
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    lp.marginEnd = dp(6)
                    layoutParams = lp
                    setPadding(dp(8), dp(10), dp(8), dp(10))
                    setOnClickListener {
                        settings.setVoiceType(type)
                        voiceTypeLabel.text = "Current: $type"
                        tts.speak("This is what my $type voice sounds like.")
                    }
                })
            }
            addView(voiceRow)
        })

        root.addView(sectionCard("VOICE SPEED") {
            val speedLabel = body("${"%.1f".format(settings.getVoiceSpeed())}x")
            addView(speedLabel)
            addView(SeekBar(this@SettingsActivity).apply {
                max = 20
                progress = ((settings.getVoiceSpeed() - 0.3f) * 10).toInt().coerceIn(0, 20)
                setPadding(0, dp(8), 0, dp(4))
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
        })

        root.addView(sectionCard("YOUR NAME") {
            val nameInput = EditText(this@SettingsActivity).apply {
                setText(settings.getUserName())
                setTextColor(C_WHITE)
                setHintTextColor(C_MUTED)
                typeface = Typeface.MONOSPACE
                textSize = 14f
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#122230"))
                    cornerRadius = dp(8).toFloat()
                    setStroke(dp(1), C_BORDER)
                }
                setPadding(dp(12), dp(12), dp(12), dp(12))
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.topMargin = dp(8)
                layoutParams = lp
            }
            addView(nameInput)
            addView(Button(this@SettingsActivity).apply {
                text = "Save Name"
                styleButton(this)
                setOnClickListener {
                    val name = nameInput.text.toString().trim()
                    if (name.isNotBlank()) {
                        settings.setUserName(name)
                        toast("Saved as $name")
                    }
                }
            })
        })

        root.addView(sectionCard("REMEMBERED NOTE") {
            noteText = body(memory.recall("last_note") ?: "(nothing remembered yet)")
            addView(noteText)
            addView(Button(this@SettingsActivity).apply {
                text = "Clear Note"
                styleButton(this)
                setOnClickListener {
                    memory.forget("last_note")
                    noteText.text = "(nothing remembered yet)"
                }
            })
        })

        root.addView(sectionCard("VOICE AUTHENTICATION") {
            addView(muted(
                "Approximate on-device voice matching — good enough to reject an obviously different voice. Checked once per session."
            ))
            voiceAuthStatus = body(voiceAuthStatusText())
            addView(voiceAuthStatus)
            addView(Button(this@SettingsActivity).apply {
                text = "🎙  Enroll My Voice"
                styleButton(this, filled = true)
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
            val voiceAuthToggle = Button(this@SettingsActivity).apply {
                text = if (voiceAuth.isEnabled()) "Disable Voice Lock" else "Enable Voice Lock"
                styleButton(this)
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
            addView(voiceAuthToggle)
            addView(Button(this@SettingsActivity).apply {
                text = "Reset Voice Enrollment"
                styleButton(this)
                setOnClickListener {
                    voiceAuth.resetEnrollment()
                    voiceAuthStatus.text = voiceAuthStatusText()
                    voiceAuthToggle.text = "Enable Voice Lock"
                    toast("Voice enrollment cleared.")
                }
            })
        })

        root.addView(sectionCard("LOCK SCREEN") {
            addView(muted(
                "Android blocks apps from changing the device lock PIN. This opens system security settings. For per-app lock, use voice: \"lock whatsapp\"."
            ))
            addView(Button(this@SettingsActivity).apply {
                text = "Open Lock Screen Settings"
                styleButton(this)
                setOnClickListener {
                    try {
                        startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                    } catch (e: Exception) {
                        toast("Couldn't open security settings on this device.")
                    }
                }
            })
        })

        root.addView(Button(this).apply {
            text = "←  Close"
            styleButton(this)
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply {
            setBackgroundColor(C_BG)
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
