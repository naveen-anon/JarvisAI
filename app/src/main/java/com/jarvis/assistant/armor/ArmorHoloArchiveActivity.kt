package com.jarvis.assistant.armor

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.ui.CornerFrameView
import com.jarvis.assistant.ui.HudOverlayView

/**
 * Full-screen Iron Man 3 style Hall of Armor.
 * Pan / pinch / long-press-drag suits on a cyan holographic grid.
 *
 * Header + close chip use the same "jg" card language as the rest of the
 * app (icon badge, two-tone title, pill chip) so this screen matches
 * Usage Stats / Settings / Voice Auth instead of standing alone.
 */
class ArmorHoloArchiveActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.parseColor("#020B14")
        window.navigationBarColor = Color.parseColor("#020B14")

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#020B14"))
        }

        // Faint scanning grid behind everything, matching MainActivity/Settings/etc.
        root.addView(
            HudOverlayView(this).apply { alpha = 0.18f },
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        val grid = HoloArmorGridView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            listener = object : HoloArmorGridView.Listener {
                override fun onSuitSelected(mark: ArmorMark) {
                    // soft feedback only
                }
                override fun onSuitActivated(mark: ArmorMark) {
                    // Double-tap → open detail
                    startActivity(
                        Intent(this@ArmorHoloArchiveActivity, ArmorDetailActivity::class.java)
                            .putExtra(ArmorDetailActivity.EXTRA_MARK, mark.number)
                    )
                }
            }
        }
        root.addView(grid)

        // Header: icon badge + two-tone title + subtitle, same pattern as
        // Usage Stats / Settings / Voice Auth headers.
        val dp = resources.displayMetrics.density
        fun px(v: Int) = (v * dp).toInt()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(16), px(14), px(16), px(10))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#E6020B14"), Color.parseColor("#00020B14"))
            )
        }

        val iconBadge = TextView(this).apply {
            text = "\uD83D\uDEE1"
            textSize = 20f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#1400E5FF"))
                setStroke(px(2), Color.parseColor("#8800E5FF"))
            }
        }
        header.addView(iconBadge, LinearLayout.LayoutParams(px(46), px(46)))

        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(12), 0, 0, 0)
        }
        val titleRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        titleRow.addView(TextView(this).apply {
            text = "ARMOR "
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 17f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        titleRow.addView(TextView(this).apply {
            text = "ARCHIVE"
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        })
        titleCol.addView(titleRow)
        titleCol.addView(TextView(this).apply {
            text = "Hall of Armor · pan, pinch, tap to select"
            setTextColor(Color.parseColor("#7AB8C8"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
        })
        header.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Close chip — pill button matching bg_jg_button_outline style
        val close = TextView(this).apply {
            text = "✕  CLOSE"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            setPadding(px(14), px(8), px(14), px(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = px(14).toFloat()
                setColor(Color.TRANSPARENT)
                setStroke(px(2), Color.parseColor("#9900E5FF"))
            }
            setOnClickListener { finish() }
        }
        header.addView(close, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(
            header,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.TOP
            }
        )

        // Screen-level corner brackets + ticks, same chrome as MainActivity/Settings.
        root.addView(
            CornerFrameView(this),
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        )

        setContentView(root)
    }
}
