package com.jarvis.assistant.settings

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
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
 * Usage Stats screen — real vector icons (no emoji), matching the
 * reference design 1:1: icon-badge header, metric cards with waveform /
 * streak-dots, colored info cards, quick-insights card, outline close.
 */
class StatsActivity : AppCompatActivity() {

    private val C_CYAN = Color.parseColor("#00E5FF")
    private val C_TEXT = Color.parseColor("#B8ECFF")
    private val C_MUTED = Color.parseColor("#7AB8C8")
    private val C_WHITE = Color.parseColor("#F0FBFF")
    private val C_PURPLE = Color.parseColor("#C084FC")
    private val C_GREEN = Color.parseColor("#22C55E")
    private val C_PINK = Color.parseColor("#EC4899")

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

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

    /** Real vector icon inside a colored ring badge (replaces emoji icons). */
    private fun iconBadge(iconRes: Int, ringRes: Int, tint: Int, size: Int = 52, iconSize: Int = 24) =
        FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(size), dp(size))
            background = ContextCompat.getDrawable(this@StatsActivity, ringRes)
            addView(ImageView(this@StatsActivity).apply {
                setImageDrawable(ContextCompat.getDrawable(this@StatsActivity, iconRes))
                setColorFilter(tint, PorterDuff.Mode.SRC_IN)
                layoutParams = FrameLayout.LayoutParams(dp(iconSize), dp(iconSize)).apply {
                    gravity = Gravity.CENTER
                }
            })
        }

    private fun smallIcon(iconRes: Int, tint: Int, size: Int = 16) =
        ImageView(this).apply {
            setImageDrawable(ContextCompat.getDrawable(this@StatsActivity, iconRes))
            setColorFilter(tint, PorterDuff.Mode.SRC_IN)
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

        // ===== Header =====
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(iconBadge(R.drawable.ic_bar_chart, R.drawable.bg_icon_ring_cyan, C_CYAN))

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
            gravity = Gravity.CENTER_HORIZONTAL
            background = jgChip()
            setPadding(dp(10), dp(8), dp(10), dp(8))
            addView(smallIcon(R.drawable.ic_track, C_GREEN, 16))
            addView(TextView(this@StatsActivity).apply {
                text = "Track"; setTextColor(C_TEXT); textSize = 9f; typeface = Typeface.MONOSPACE
                setPadding(0, dp(3), 0, 0)
            })
            addView(TextView(this@StatsActivity).apply {
                text = "Learn"; setTextColor(C_TEXT); textSize = 9f; typeface = Typeface.MONOSPACE
            })
            addView(TextView(this@StatsActivity).apply {
                text = "Improve"; setTextColor(C_TEXT); textSize = 9f; typeface = Typeface.MONOSPACE
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
            addView(iconBadge(R.drawable.ic_terminal, R.drawable.bg_icon_ring_cyan, C_CYAN, size = 40, iconSize = 18))
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
            addView(iconBadge(R.drawable.ic_flame, R.drawable.bg_icon_ring_purple, C_PURPLE, size = 40, iconSize = 18))
            addView(spacer(8))
            addView(TextView(this@StatsActivity).apply {
                text = "DAY STREAK"; setTextColor(C_PURPLE); textSize = 11f
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
                text = "${stats.currentStreak}"; setTextColor(C_PURPLE); textSize = 34f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            })
            streakRow.addView(smallIcon(R.drawable.ic_flame, Color.parseColor("#FB923C"), 20).apply {
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginStart = dp(8) }
            })
            addView(streakRow)
            addView(spacer(10))
            addView(dotProgressRow(filled = stats.currentStreak.coerceIn(0, 6)))
        })
        root.addView(metricsRow)
        root.addView(spacer(20))

        // ===== APP USAGE section =====
        root.addView(sectionHeader(R.drawable.ic_bar_chart, "APP USAGE", "Your most used apps (from Jarvis)"))
        root.addView(spacer(10))

        root.addView(infoCard(
            bg = R.drawable.bg_jg_card_green,
            ring = R.drawable.bg_icon_ring_green,
            icon = R.drawable.ic_star_filled,
            title = "MOST USED APPS",
            titleColor = C_GREEN,
            subtitle = if (stats.topApps.isEmpty()) "Not enough data yet – keep using Jarvis!"
                       else stats.topApps.joinToString(", ") { it.first }
        ))
        root.addView(spacer(10))
        root.addView(infoCard(
            bg = R.drawable.bg_jg_card_pink,
            ring = R.drawable.bg_icon_ring_pink,
            icon = R.drawable.ic_people,
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
            hRow.addView(iconBadge(R.drawable.ic_lightbulb, R.drawable.bg_icon_ring_cyan, C_CYAN, size = 34, iconSize = 16))
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
                addView(smallIcon(R.drawable.ic_info, C_CYAN, 16).apply {
                    layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(8) }
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
        val closeBtn = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = jgButtonOutline()
            setPadding(0, dp(14), 0, dp(14))
            setOnClickListener { finish() }
        }
        closeBtn.addView(smallIcon(R.drawable.ic_chevron_right, C_CYAN, 16).apply {
            rotation = 180f
            layoutParams = LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(8) }
        })
        closeBtn.addView(TextView(this).apply {
            text = "CLOSE"; setTextColor(C_CYAN); textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        root.addView(closeBtn)

        return ScrollView(this).apply { addView(root) }
    }

    private fun spacer(hDp: Int) = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(hDp))
    }

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
                    layoutParams = LinearLayout.LayoutParams(dp(3), dp(h)).apply { marginEnd = dp(2) }
                })
            }
        }
    }

    private fun dotProgressRow(filled: Int, total: Int = 7): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            for (i in 0 until total) {
                addView(View(this@StatsActivity).apply {
                    val on = i < filled
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(if (on) C_PURPLE else Color.parseColor("#33FFFFFF"))
                    }
                    layoutParams = LinearLayout.LayoutParams(dp(if (on) 10 else 7), dp(if (on) 10 else 7)).apply {
                        marginEnd = dp(6)
                    }
                })
            }
        }
    }

    private fun sectionHeader(iconRes: Int, title: String, subtitle: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(smallIcon(iconRes, C_CYAN, 18))
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

    private fun infoCard(bg: Int, ring: Int, icon: Int, title: String, titleColor: Int, subtitle: String) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = jgCard(bg)
            setPadding(dp(12), dp(12), dp(12), dp(12))

            addView(iconBadge(icon, ring, titleColor, size = 40, iconSize = 18))
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
            addView(smallIcon(R.drawable.ic_chevron_right, titleColor, 20))
        }
}
