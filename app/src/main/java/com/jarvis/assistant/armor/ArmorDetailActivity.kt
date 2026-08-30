package com.jarvis.assistant.armor

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R

class ArmorDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val num = intent.getIntExtra(EXTRA_MARK, 42)
        val mark = ArmorCatalog.byNumber(num) ?: ArmorCatalog.byNumber(42)!!

        val primary = Color.parseColor(mark.primaryHex)
        val secondary = Color.parseColor(mark.secondaryHex)
        val cyan = Color.parseColor("#00E5FF")
        val textMain = Color.parseColor("#E0F4FA")
        val textDim = Color.parseColor("#7AA8B8")

        window.statusBarColor = Color.parseColor("#020810")
        window.navigationBarColor = Color.parseColor("#020810")

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#020810"))
            isFillViewport = true
        }
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(28), dp(16), dp(40))
        }

        // Header
        col.addView(TextView(this).apply {
            text = "J.A.R.V.I.S  ·  ARMOR ARCHIVE"
            setTextColor(cyan)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.14f
            setPadding(dp(8), 0, 0, dp(12))
        })

        // ===== HERO glass: full suit =====
        col.addView(glassCard {
            addView(TextView(this@ArmorDetailActivity).apply {
                text = "MARK ${mark.roman}"
                setTextColor(primary)
                textSize = 26f
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(this@ArmorDetailActivity).apply {
                text = mark.codename.uppercase()
                setTextColor(secondary)
                textSize = 14f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(0, dp(4), 0, 0)
            })
            addView(TextView(this@ArmorDetailActivity).apply {
                text = mark.era
                setTextColor(textDim)
                textSize = 11f
                gravity = Gravity.CENTER
                setPadding(0, dp(2), 0, dp(8))
            })

            // Full suit preview
            val suitBox = LinearLayout(this@ArmorDetailActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, dp(8))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#22000000"))
                    cornerRadius = dp(16).toFloat()
                    setStroke(dp(1), Color.parseColor("#3300E5FF"))
                }
            }
            // Prefer a real armor_mark_N image if one was dropped into res/drawable
            // (with a slow Ken Burns zoom for motion); otherwise fall back to an
            // original animated silhouette tinted with this mark's own colors —
            // pulsing chest reactor + idle sway, distinct per mark via color/glow.
            val pngId = resources.getIdentifier(
                "armor_mark_${mark.number}", "drawable", packageName
            )
            if (pngId != 0) {
                val suitImg = ImageView(this@ArmorDetailActivity).apply {
                    setImageResource(pngId)
                    clearColorFilter()
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    layoutParams = LinearLayout.LayoutParams(dp(200), dp(280))
                }
                suitBox.addView(suitImg)
                suitImg.post {
                    val zoom = android.animation.ValueAnimator.ofFloat(1f, 1.08f).apply {
                        duration = 4000
                        repeatMode = android.animation.ValueAnimator.REVERSE
                        repeatCount = android.animation.ValueAnimator.INFINITE
                        addUpdateListener {
                            val s = it.animatedValue as Float
                            suitImg.scaleX = s
                            suitImg.scaleY = s
                        }
                    }
                    zoom.start()
                }
            } else {
                val silhouette = ArmorSilhouetteView(this@ArmorDetailActivity).apply {
                    primaryColor = primary
                    secondaryColor = secondary
                    layoutParams = LinearLayout.LayoutParams(dp(200), dp(280))
                }
                suitBox.addView(silhouette)
            }
            addView(suitBox)

            // Color legend
            addView(LinearLayout(this@ArmorDetailActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(0, dp(12), 0, 0)
                addView(dot(primary))
                addView(spacer(10))
                addView(dot(secondary))
            })
        })

        // Overview
        col.addView(glassCard {
            addView(label("OVERVIEW"))
            addView(TextView(this@ArmorDetailActivity).apply {
                text = mark.summary
                setTextColor(textMain)
                textSize = 14f
                setLineSpacing(dp(2).toFloat(), 1.2f)
            })
        })

        // Structure
        col.addView(glassCard {
            addView(label("STRUCTURE & SYSTEMS"))
            mark.structure.forEach { line ->
                addView(TextView(this@ArmorDetailActivity).apply {
                    text = "▸  $line"
                    setTextColor(cyan)
                    textSize = 13f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, dp(6), 0, dp(6))
                })
            }
        })

        col.addView(TextView(this).apply {
            text = "  ←  CLOSE"
            setTextColor(cyan)
            textSize = 15f
            typeface = Typeface.MONOSPACE
            setPadding(dp(8), dp(24), 0, 0)
            setOnClickListener { finish() }
        })

        root.addView(col)
        setContentView(root)
    }

    private fun label(t: String) = TextView(this).apply {
        text = t
        setTextColor(Color.parseColor("#00E5FF"))
        textSize = 11f
        typeface = Typeface.MONOSPACE
        letterSpacing = 0.12f
        setPadding(0, 0, 0, dp(10))
    }

    private fun dot(c: Int) = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(18), dp(18))
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(c)
            setStroke(dp(1), Color.parseColor("#88FFFFFF"))
        }
    }

    private fun spacer(w: Int) = TextView(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(w), 1)
    }

    private fun glassCard(build: LinearLayout.() -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.liquid_glass_card)
            setPadding(dp(18), dp(16), dp(18), dp(16))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(12)
            layoutParams = lp
            build()
        }
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_MARK = "mark_number"
    }
}
