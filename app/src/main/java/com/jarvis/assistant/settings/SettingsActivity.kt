package com.jarvis.assistant.settings

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.util.PersistentMemory
import com.jarvis.assistant.util.SettingsManager
import com.jarvis.assistant.voice.TextToSpeechHelper

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
    private lateinit var voiceTypeLabel: TextView
    private lateinit var noteText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsManager(this)
        memory = PersistentMemory(this)
        tts = TextToSpeechHelper(this)
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

        root.addView(Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply { addView(root) }
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
