package com.jarvis.assistant.settings

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.CornerFrameView
import com.jarvis.assistant.util.FeedbackClient
import com.jarvis.assistant.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeedbackActivity : AppCompatActivity() {
    private val client = FeedbackClient()
    private val settings by lazy { SettingsManager(this) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedRating = 0
    private lateinit var starViews: List<TextView>

    private val cyan = Color.parseColor("#00E5FF")
    private val hudTextDim = Color.parseColor("#5A8A9A")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this).apply {
            setBackgroundResource(R.drawable.glass_screen_bg)
            addView(CornerFrameView(this@FeedbackActivity))
            addView(buildUi())
        })
    }

    private fun glassField(hintText: String, multiline: Boolean = false) = EditText(this).apply {
        hint = hintText
        setHintTextColor(hudTextDim)
        setTextColor(Color.WHITE)
        textSize = 14f
        typeface = Typeface.MONOSPACE
        background = ContextCompat.getDrawable(this@FeedbackActivity, R.drawable.glass_panel_bg)
        setPadding(28, 24, 28, 24)
        if (multiline) {
            minLines = 3
            gravity = Gravity.TOP
        }
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = 8
        layoutParams = lp
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 56, 40, 80)
        }

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(View(this@FeedbackActivity).apply {
                layoutParams = LinearLayout.LayoutParams(8, 48)
                background = ContextCompat.getDrawable(this@FeedbackActivity, R.drawable.hud_accent_bar)
            })
            addView(TextView(this@FeedbackActivity).apply {
                text = "  SEND FEEDBACK"
                setTextColor(Color.WHITE)
                textSize = 20f
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.05f
            })
        })
        root.addView(TextView(this).apply {
            text = "Bugs, ideas, or just how it's going — goes straight to the developer."
            setTextColor(hudTextDim)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, 8, 0, 20)
        })

        fun label(t: String) = TextView(this).apply {
            text = t.uppercase()
            setTextColor(cyan)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
            setPadding(0, 20, 0, 8)
        }

        root.addView(label("Rating (optional)"))
        val starRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(this@FeedbackActivity, R.drawable.glass_panel_bg)
            setPadding(20, 16, 20, 16)
        }
        starViews = (1..5).map { i ->
            TextView(this).apply {
                text = "\u2606"
                textSize = 28f
                setTextColor(hudTextDim)
                setPadding(10, 0, 10, 0)
                setOnClickListener {
                    selectedRating = if (selectedRating == i) 0 else i
                    updateStars()
                }
            }
        }
        starViews.forEach { starRow.addView(it) }
        root.addView(starRow)

        root.addView(label("Message *"))
        val messageInput = glassField("What's on your mind?", multiline = true)
        root.addView(messageInput)

        root.addView(label("Name (optional)"))
        val nameInput = glassField("Anonymous")
        root.addView(nameInput)

        root.addView(label("Contact (optional)"))
        val contactInput = glassField("Email or phone, if you want a reply")
        root.addView(contactInput)

        val sendBtn = Button(this).apply {
            text = "SEND FEEDBACK"
            isAllCaps = false
            background = ContextCompat.getDrawable(this@FeedbackActivity, R.drawable.glass_button_filled)
            setTextColor(Color.parseColor("#03080E"))
            typeface = Typeface.MONOSPACE
            textSize = 14f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 28
            layoutParams = lp
            setOnClickListener {
                val msg = messageInput.text.toString().trim()
                if (msg.length < 3) {
                    Toast.makeText(this@FeedbackActivity, "Message needs at least 3 characters.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                isEnabled = false
                text = "SENDING\u2026"
                scope.launch {
                    val ok = client.send(
                        message = msg,
                        name = nameInput.text.toString().trim().ifBlank { null },
                        contact = contactInput.text.toString().trim().ifBlank { null },
                        rating = selectedRating.takeIf { it > 0 }
                    )
                    if (ok) {
                        settings.markFeedbackGiven()
                        Toast.makeText(this@FeedbackActivity, "Thanks! Feedback sent.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@FeedbackActivity, "Couldn't send — check connection.", Toast.LENGTH_SHORT).show()
                        isEnabled = true
                        text = "SEND FEEDBACK"
                    }
                }
            }
        }
        root.addView(sendBtn)

        root.addView(Button(this).apply {
            text = "CANCEL"
            isAllCaps = false
            background = ContextCompat.getDrawable(this@FeedbackActivity, R.drawable.glass_button_bg)
            setTextColor(cyan)
            typeface = Typeface.MONOSPACE
            textSize = 13f
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.topMargin = 12
            layoutParams = lp
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun updateStars() {
        starViews.forEachIndexed { i, star ->
            val filled = i < selectedRating
            star.text = if (filled) "\u2605" else "\u2606"
            star.setTextColor(if (filled) Color.parseColor("#FFB300") else hudTextDim)
        }
    }
}
