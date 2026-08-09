package com.jarvis.assistant.settings

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(com.jarvis.assistant.R.drawable.glass_screen_bg)
            setPadding(48, 64, 48, 64)
        }

        fun label(t: String) = TextView(this).apply {
            text = t
            setTextColor(Color.parseColor("#8FC7D6"))
            textSize = 13f
            setPadding(0, 24, 0, 6)
        }

        root.addView(TextView(this).apply {
            text = "Send Feedback"
            setTextColor(Color.WHITE)
            textSize = 22f
        })
        root.addView(TextView(this).apply {
            text = "Bugs, ideas, or just how it's going — goes straight to the developer."
            setTextColor(Color.parseColor("#5C8A94"))
            textSize = 12f
            setPadding(0, 8, 0, 0)
        })

        root.addView(label("Rating (optional)"))
        val starRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        starViews = (1..5).map { i ->
            TextView(this).apply {
                text = "☆"
                textSize = 28f
                setTextColor(Color.parseColor("#4A6B75"))
                setPadding(8, 0, 8, 0)
                setOnClickListener {
                    selectedRating = if (selectedRating == i) 0 else i
                    updateStars()
                }
            }
        }
        starViews.forEach { starRow.addView(it) }
        root.addView(starRow)

        root.addView(label("Message *"))
        val messageInput = EditText(this).apply {
            hint = "What's on your mind?"
            setHintTextColor(Color.parseColor("#4A6B75"))
            setTextColor(Color.WHITE)
            minLines = 3
            gravity = Gravity.TOP
        }
        root.addView(messageInput)

        root.addView(label("Name (optional)"))
        val nameInput = EditText(this).apply {
            hint = "Anonymous"
            setHintTextColor(Color.parseColor("#4A6B75"))
            setTextColor(Color.WHITE)
        }
        root.addView(nameInput)

        root.addView(label("Contact (optional)"))
        val contactInput = EditText(this).apply {
            hint = "Email or phone, if you want a reply"
            setHintTextColor(Color.parseColor("#4A6B75"))
            setTextColor(Color.WHITE)
        }
        root.addView(contactInput)

        val sendBtn = Button(this).apply {
            text = "Send Feedback"
            setBackgroundResource(com.jarvis.assistant.R.drawable.glass_button_filled)
            setTextColor(Color.parseColor("#03080E"))
            setOnClickListener {
                val msg = messageInput.text.toString().trim()
                if (msg.length < 3) {
                    Toast.makeText(this@FeedbackActivity, "Message needs at least 3 characters.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                isEnabled = false
                text = "Sending…"
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
                        text = "Send Feedback"
                    }
                }
            }
        }
        root.addView(sendBtn)

        root.addView(Button(this).apply {
            text = "Cancel"
            setBackgroundResource(com.jarvis.assistant.R.drawable.glass_button_bg)
            setTextColor(Color.parseColor("#00E5FF"))
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun updateStars() {
        starViews.forEachIndexed { i, star ->
            val filled = i < selectedRating
            star.text = if (filled) "★" else "☆"
            star.setTextColor(if (filled) Color.parseColor("#FFB300") else Color.parseColor("#4A6B75"))
        }
    }
}
