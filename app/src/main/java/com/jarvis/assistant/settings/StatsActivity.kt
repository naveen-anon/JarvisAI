package com.jarvis.assistant.settings

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.CornerFrameView
import com.jarvis.assistant.ui.HudOverlayView
import com.jarvis.assistant.util.AutoLearnEngine

/**
 * Usage Stats screen — matches the app-wide "jg" card language:
 * icon-badge header, side-by-side metric cards, colored info cards
 * (green = app usage, pink = contacts), quick-insights card, outline
 * close button. Same building blocks as SettingsActivity/LockScreenActivity.
 */
class StatsActivity : AppCompatActivity() {

    private val C_CYAN = Color.parseColor("#00E5FF")
    private val C_TEXT = Color.parseColor("#B8ECFF")
    private val C_MUTED = Color.parseColor("#7AB8C8")
    private val C_WHITE = Color.parseColor("#F0FBFF")
    private val C_GREEN = Color.parseColor("#22C55E")
    private val C_PINK = Color.parseColor("#EC4899")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun sp(v: Float) = v

    private fun iconRing(colorRes: Int) = ContextCompat.getDrawable(this, colorRes)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stats = AutoLearnEngine(this).getUsageStats()

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#020810"))
            addView(
                HudOverlayView(this@StatsActivity).apply { alpha = 0.18f },
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )
            addView(buildUi(stats))
            addView(
                CornerFrameView(this@StatsActivity),
                FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            )
        }
        setContentView(root)
    }

    /** Small circular icon badge like the header icons across the app. */
    private fun iconBadge(emoji: String, ringDrawable: Int, size: Int = 52, textSize: Float = 20f) =
        TextView(this).apply {
            text = emoji
            this.textSize = textSize
            gravity = Gravity.CENTER
            background = iconRing(ringDrawable)
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
        }

    private fun jgCard(bgRes: Int = R.drawable.bg_jg_card) = ContextCompat.getDrawable(this, bgRes)
    private fun jgChip() = ContextCompat.getDrawable(this, R.drawable.bg_jg_chip)
    private fun jgButtonOutline() = ContextCompat.getDrawable(this, R.drawable.bg_jg_button_outline)

    private fun buildUi(stats: AutoLearnEngine.UsageStats): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }

        // ===== Header: icon badge + two-tone title + subtitle, chip top-right =====
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(iconBadge("\uD83D\uDCCA", R.drawable.bg_icon_ring_cyan))

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        titleRow.addView(TextView(this).apply {
            text = "USAGE "
            setTextColor(C_CYAN); textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        titleRow.addView(TextView(this).apply {
            text = "STATS"
            setTextColor(C_WHITE); textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        titleCol.addView(titleRow)
        titleCol.addView(TextView(this).apply {
            text = "Your Jarvis activity at a glance"
            setTextColor(C_MUTED); textSize = 11f
            typeface = Typeface.MONOSPACE
        })
        headerRow.addView(titleCol)

        headerRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = jgChip()
            setPadding(dp(10), dp(8), dp(10), dp(8))
            addView(TextView(this@StatsActivity).apply {
                text = "Track"; setTextColor(C_TEXT); textSize = 10f; typeface = Typeface.MONOSPACE
            })
            addView(TextView(this@StatsActivity).apply {
                text = "Learn"; setTextColor(C_TEXT); textSize = 10f; typeface = Typeface.MONOSPACE
            })
            addView(TextView(this@StatsActivity).apply {
                text = "Improve"; setTextColor(C_TEXT); textSize = 10f; typeface = Typeface.MONOSPACE
            })
        })
        root.addView(headerRow)
        root.addView(spacer(16))

        // ===== Two metric cards =====
        val metricsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

        metricsRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = jgCard()
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginEnd = dp(8) }
            addView(iconBadge("\uD83D\uDCC8", R.drawable.bg_icon_ring_cyan, size = 40, textSize = 16f))
            addView(spacer(8))
            addView(TextView(this@StatsActivity).apply {
                text = "TOTAL COMMANDS"; setTextColor(C_CYAN); textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
            addView(TextView(this@StatsActivity).apply {
                text = "Commands executed today"; setTextColor(C_MUTED); textSize = 9f; typeface = Typeface.MONOSPACE
            })
            addView(TextView(this@StatsActivity).apply {
                text = stats.totalInteractions.toString()
                setTextColor(C_CYAN); textSize = 34f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                setPadding(0, dp(8), 0, dp(10))
            })
            addView(waveformRow())
        })

        metricsRow.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = jgCard(R.drawable.bg_jg_card_purple)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            addView(iconBadge("\uD83D\uDCC4", R.drawable.bg_icon_ring_purple, size = 40, textSize = 16f))
            addView(spacer(8))
            addView(TextView(this@StatsActivity).apply {
                text = "DAY STREAK"; setTextColor(Color.parseColor("#C084FC")); textSize = 11f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
            addView(TextView(this@StatsActivity).apply {
                text = "Consecutive days active"; setTextColor(C_MUTED); textSize = 9f; typeface = Typeface.MONOSPACE
            })
            val streakRow = LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(8), 0, 0)
            }
            streakRow.addView(TextView(this@StatsActivity).apply {
                text = "${stats.currentStreak}"; setTextColor(Color.parseColor("#C084FC")); textSize = 34f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
            streakRow.addView(TextView(this@StatsActivity).apply {
                text = "  \uD83D\uDD25"; textSize = 22f
            })
            addView(streakRow)
            addView(spacer(10))
            addView(dotProgressRow(filled = stats.currentStreak.coerceIn(0, 6)))
        })
        root.addView(metricsRow)
        root.addView(spacer(20))

        // ===== APP USAGE section =====
        root.addView(sectionHeader("\uD83D\uDD32", "APP USAGE", "Your most used apps (from Jarvis)"))
        root.addView(spacer(10))

        root.addView(infoCard(
            bg = R.drawable.bg_jg_card_green,
            ring = R.drawable.bg_icon_ring_green,
            emoji = "\u2B50",
            title = "MOST USED APPS",
            titleColor = C_GREEN,
            subtitle = if (stats.topApps.isEmpty()) "Not enough data yet – keep using Jarvis!"
                       else stats.topApps.joinToString(", ") { it.first }
        ))
        root.addView(spacer(10))
        root.addView(infoCard(
            bg = R.drawable.bg_jg_card_pink,
            ring = R.drawable.bg_icon_ring_pink,
            emoji = "\uD83D\uDC65",
            title = "MOST CONTACTED",
            titleColor = C_PINK,
            subtitle = if (stats.topContacts.isEmpty()) "Not enough data yet."
                       else stats.topContacts.joinToString(", ") { it.first }
        ))
        root.addView(spacer(20))

        // ===== QUICK INSIGHTS =====
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = jgCard()
            setPadding(dp(14), dp(14), dp(14), dp(14))

            val hRow = LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            hRow.addView(iconBadge("\uD83D\uDCA1", R.drawable.bg_icon_ring_cyan, size = 34, textSize = 14f))
            hRow.addView(TextView(this@StatsActivity).apply {
                text = "  QUICK INSIGHTS"; setTextColor(C_CYAN); textSize = 13f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
            addView(hRow)
            addView(spacer(10))

            addView(LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                background = jgChip()
                setPadding(dp(10), dp(10), dp(10), dp(10))
                addView(TextView(this@StatsActivity).apply {
                    text = "\u24D8  "; setTextColor(C_CYAN); textSize = 13f
                })
                addView(TextView(this@StatsActivity).apply {
                    text = "Keep using Jarvis to unlock detailed insights, app stats, and more in the future!"
                    setTextColor(C_TEXT); textSize = 12f; typeface = Typeface.MONOSPACE
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                })
            })
        })
        root.addView(spacer(20))

        // ===== Close button =====
        root.addView(TextView(this).apply {
            text = "\u2190  CLOSE"
            setTextColor(C_CYAN)
            textSize = 14f
            gravity = Gravity.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            background = jgButtonOutline()
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { finish() }
        })

        return ScrollView(this).apply { addView(root) }
    }

    private fun spacer(hDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(hDp))
    }

    /** Small equalizer-style bar chart, like the strip under Total Commands. */
    private fun waveformRow(): LinearLayout {
        val heights = intArrayOf(6, 12, 8, 16, 10, 18, 9, 14, 7, 20, 11, 15, 8, 17, 10, 13, 6, 19, 9, 12)
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(24))
            heights.forEach { h ->
                addView(View(this@StatsActivity).apply {
                    setBackgroundColor(C_CYAN)
                    alpha = 0.55f
                    layoutParams = LinearLayout.LayoutParams(dp(3), dp(h)).apply {
                        marginEnd = dp(2)
                    }
                })
            }
        }
    }

    /** Row of small dots marking streak progress, like the strip under Day Streak. */
    private fun dotProgressRow(filled: Int, total: Int = 7): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            for (i in 0 until total) {
                addView(View(this@StatsActivity).apply {
                    val on = i < filled
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(if (on) Color.parseColor("#C084FC") else Color.parseColor("#33FFFFFF"))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(if (on) 10 else 7), dp(if (on) 10 else 7)).apply {
                        marginEnd = dp(6)
                    }
                })
            }
        }
    }

    private fun sectionHeader(emoji: String, title: String, subtitle: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(TextView(this@StatsActivity).apply {
                text = emoji; setTextColor(C_CYAN); textSize = 14f
            })
            addView(LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), 0, 0, 0)
                addView(TextView(this@StatsActivity).apply {
                    text = title; setTextColor(C_CYAN); textSize = 15f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                })
                addView(TextView(this@StatsActivity).apply {
                    text = subtitle; setTextColor(C_MUTED); textSize = 10f; typeface = Typeface.MONOSPACE
                })
            })
        }

    private fun infoCard(bg: Int, ring: Int, emoji: String, title: String, titleColor: Int, subtitle: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = jgCard(bg)
            setPadding(dp(12), dp(12), dp(12), dp(12))

            addView(iconBadge(emoji, ring, size = 40, textSize = 16f))
            addView(LinearLayout(this@StatsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@StatsActivity).apply {
                    text = title; setTextColor(titleColor); textSize = 13f
                    typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                })
                addView(TextView(this@StatsActivity).apply {
                    text = subtitle; setTextColor(C_TEXT); textSize = 10f; typeface = Typeface.MONOSPACE
                    maxLines = 1
                })
            })
            addView(TextView(this@StatsActivity).apply {
                text = "\u203A"; setTextColor(titleColor); textSize = 20f
            })
        }
}
