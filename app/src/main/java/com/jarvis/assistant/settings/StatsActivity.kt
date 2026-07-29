package com.jarvis.assistant.settings

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.util.AutoLearnEngine

/**
 * Surfaces AutoLearnEngine's data as a readable dashboard — this data already existed
 * (top apps, top contacts) but was previously only ever spoken aloud in a single sentence
 * via "my routine". This makes it something you can actually look at and feel motivated by.
 */
class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stats = AutoLearnEngine(this).getUsageStats()
        setContentView(buildUi(stats))
    }

    private fun buildUi(stats: AutoLearnEngine.UsageStats): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050A0F"))
            setPadding(48, 64, 48, 64)
        }

        fun title(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#00D4FF"))
            textSize = 16f
            setPadding(0, 32, 0, 8)
        }
        fun bigNumber(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 36f
        }
        fun caption(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#5C8A94"))
            textSize = 12f
        }
        fun listItem(text: String) = TextView(this).apply {
            this.text = text
            setTextColor(Color.parseColor("#8FC7D6"))
            textSize = 14f
            setPadding(0, 4, 0, 4)
        }

        root.addView(TextView(this).apply {
            text = "Usage Stats"
            setTextColor(Color.WHITE)
            textSize = 22f
        })

        // --- Headline numbers ---
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val col1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col1.addView(bigNumber(stats.totalInteractions.toString()))
        col1.addView(caption("total commands"))
        val col2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        col2.addView(bigNumber("${stats.currentStreak}🔥"))
        col2.addView(caption("day streak"))
        row.addView(col1)
        row.addView(col2)
        root.addView(row)

        root.addView(caption(
            if (stats.firstUsedDaysAgo <= 0) "You started using Jarvis today."
            else "Using Jarvis for ${stats.firstUsedDaysAgo} day${if (stats.firstUsedDaysAgo != 1) "s" else ""} " +
                "(active on ${stats.daysActive} of those)."
        ).apply { setPadding(0, 16, 0, 0) })

        // --- Top apps ---
        root.addView(title("Most Used Apps"))
        if (stats.topApps.isEmpty()) {
            root.addView(caption("Not enough data yet — keep using Jarvis!"))
        } else {
            stats.topApps.forEach { (app, count) ->
                root.addView(listItem("$app — $count time${if (count != 1) "s" else ""}"))
            }
        }

        // --- Top contacts ---
        root.addView(title("Most Contacted"))
        if (stats.topContacts.isEmpty()) {
            root.addView(caption("Not enough data yet."))
        } else {
            stats.topContacts.forEach { (contact, count) ->
                root.addView(listItem("$contact — $count time${if (count != 1) "s" else ""}"))
            }
        }

        root.addView(Button(this).apply {
            text = "Close"
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply { addView(root) }
    }
}
