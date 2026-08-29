package com.jarvis.assistant.settings

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.CornerFrameView
import com.jarvis.assistant.util.AutoLearnEngine

class StatsActivity : AppCompatActivity() {

    private val C_CYAN = Color.parseColor("#00E5FF")
    private val C_CYAN_DIM = Color.parseColor("#0B7A94")
    private val C_TEXT = Color.parseColor("#B8D4E0")
    private val C_MUTED = Color.parseColor("#5A8A9A")
    private val C_WHITE = Color.parseColor("#F0FBFF")
    private val C_AMBER = Color.parseColor("#FFB020")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun glassCard() = ContextCompat.getDrawable(this, R.drawable.glass_card_bg)
    private fun glassChip() = ContextCompat.getDrawable(this, R.drawable.glass_chip_bg)
    private fun glassButton() = ContextCompat.getDrawable(this, R.drawable.glass_button_bg)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stats = AutoLearnEngine(this).getUsageStats()
        setContentView(FrameLayout(this).apply {
            setBackgroundResource(R.drawable.glass_screen_bg)
            addView(CornerFrameView(this@StatsActivity))
            addView(buildUi(stats))
        })
    }

    private fun buildUi(stats: AutoLearnEngine.UsageStats): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(48), dp(20), dp(40))
        }

        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(View(this@StatsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(4), dp(22)).apply { marginEnd = dp(12) }
                setBackgroundColor(C_CYAN)
            })
            addView(TextView(this@StatsActivity).apply {
                text = "USAGE STATS"
                setTextColor(C_WHITE)
                textSize = 22f
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.1f
            })
        })
        root.addView(TextView(this).apply {
            text = "Your Jarvis activity at a glance"
            setTextColor(C_MUTED)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(dp(16), dp(4), 0, dp(12))
        })

        val metricsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val col1 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glassCard()
            setPadding(dp(12), dp(18), dp(12), dp(18))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
            addView(TextView(this@StatsActivity).apply {
                text = stats.totalInteractions.toString()
                setTextColor(C_CYAN)
                textSize = 28f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
            })
            addView(TextView(this@StatsActivity).apply {
                text = "commands"
                setTextColor(C_MUTED)
                textSize = 11f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }
        val col2 = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = glassCard()
            setPadding(dp(12), dp(18), dp(12), dp(18))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(TextView(this@StatsActivity).apply {
                text = "${stats.currentStreak}"
                setTextColor(C_AMBER)
                textSize = 28f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
            })
            addView(TextView(this@StatsActivity).apply {
                text = "day streak"
                setTextColor(C_MUTED)
                textSize = 11f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
        }
        metricsRow.addView(col1)
        metricsRow.addView(col2)
        root.addView(metricsRow)

        root.addView(TextView(this).apply {
            text = if (stats.firstUsedDaysAgo <= 0) "You started using Jarvis today."
            else "Using Jarvis for ${stats.firstUsedDaysAgo} day${if (stats.firstUsedDaysAgo != 1) "s" else ""} " +
                "(active on ${stats.daysActive} of those)."
            setTextColor(C_MUTED)
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setPadding(0, dp(14), 0, 0)
        })

        fun sectionHeader(title: String) = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(22), 0, dp(10))
            addView(View(this@StatsActivity).apply {
                layoutParams = LinearLayout.LayoutParams(dp(3), dp(14)).apply { marginEnd = dp(10) }
                setBackgroundColor(C_CYAN)
            })
            addView(TextView(this@StatsActivity).apply {
                text = title
                setTextColor(C_CYAN)
                textSize = 13f
                typeface = Typeface.MONOSPACE
                letterSpacing = 0.08f
            })
        }

        fun rankRow(rank: Int, name: String, count: Int) = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = glassCard()
            setPadding(dp(14), dp(12), dp(14), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(6) }

            addView(TextView(this@StatsActivity).apply {
                text = "#$rank"
                setTextColor(if (rank == 1) C_AMBER else C_CYAN_DIM)
                textSize = 12f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                background = glassChip()
                setPadding(dp(10), dp(4), dp(10), dp(4))
                layoutParams = LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(12)
                }
            })
            addView(TextView(this@StatsActivity).apply {
                text = name
                setTextColor(C_TEXT)
                textSize = 13f
                typeface = Typeface.MONOSPACE
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@StatsActivity).apply {
                text = "${count}\u00d7"
                setTextColor(C_CYAN)
                textSize = 13f
                typeface = Typeface.MONOSPACE
            })
        }

        root.addView(sectionHeader("MOST USED APPS"))
        if (stats.topApps.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "Not enough data yet — keep using Jarvis!"
                setTextColor(C_MUTED)
                textSize = 12f
                typeface = Typeface.MONOSPACE
            })
        } else {
            stats.topApps.forEachIndexed { i, (app, count) ->
                root.addView(rankRow(i + 1, app, count))
            }
        }

        root.addView(sectionHeader("MOST CONTACTED"))
        if (stats.topContacts.isEmpty()) {
            root.addView(TextView(this).apply {
                text = "Not enough data yet."
                setTextColor(C_MUTED)
                textSize = 12f
                typeface = Typeface.MONOSPACE
            })
        } else {
            stats.topContacts.forEachIndexed { i, (contact, count) ->
                root.addView(rankRow(i + 1, contact, count))
            }
        }

        root.addView(Button(this).apply {
            text = "\u2190  Close"
            background = glassButton()
            setTextColor(C_CYAN)
            isAllCaps = false
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(24) }
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply {
            addView(root)
        }
    }
}
