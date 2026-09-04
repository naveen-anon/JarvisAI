package com.jarvis.assistant.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.jarvis.assistant.R

enum class HudState { IDLE, LISTENING, THINKING, EXECUTING, SPEAKING, DONE, ERROR }

/**
 * Real Arc Reactor PNG + glow colored from PNG / settings accent.
 * No rotation.
 */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val glowBg = ImageView(context).apply {
        setImageResource(R.drawable.arc_reactor_glow)
        scaleType = ImageView.ScaleType.FIT_CENTER
        alpha = 0.55f
    }

    private val image = ImageView(context).apply {
        setImageResource(R.drawable.arc_reactor)
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
        setBackgroundColor(Color.TRANSPARENT)
    }

    private var glowAnim: ObjectAnimator? = null
    private var alphaAnim: ObjectAnimator? = null
    private var accentColor: Int = Color.parseColor("#00E5FF")

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyState()
        }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(glowBg, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyState()
    }

    /** Settings / PNG color se glow tint */
    fun setAccentColor(hex: String) {
        accentColor = try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            Color.parseColor("#00E5FF")
        }
        applyState()
    }

    private fun applyState() {
        glowAnim?.cancel()
        alphaAnim?.cancel()

        image.rotation = 0f
        glowBg.rotation = 0f
        image.clearColorFilter()

        when (state) {
            HudState.IDLE -> {
                // Dim – PNG natural color, soft glow in accent
                image.alpha = 0.78f
                glowBg.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                glowBg.alpha = 0.32f
                image.scaleX = 1f
                image.scaleY = 1f
                glowBg.scaleX = 1.06f
                glowBg.scaleY = 1.06f
            }
            HudState.LISTENING -> {
                // Full cyan energy (accent)
                image.alpha = 1f
                glowBg.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                startBreathing(0.55f, 0.95f, 650)
                softScale(1.04f, 1.20f)
            }
            HudState.THINKING -> {
                // Amber override
                val amber = Color.parseColor("#FFAA33")
                image.setColorFilter(amber, PorterDuff.Mode.MULTIPLY)
                image.alpha = 1f
                glowBg.setColorFilter(Color.parseColor("#FF8800"), PorterDuff.Mode.SRC_ATOP)
                startBreathing(0.50f, 1.0f, 420)
                softScale(1.06f, 1.24f)
            }
            HudState.EXECUTING -> {
                image.alpha = 1f
                glowBg.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                glowBg.alpha = 0.9f
            }
            HudState.DONE -> {
                image.alpha = 0.9f
                glowBg.setColorFilter(accentColor, PorterDuff.Mode.SRC_ATOP)
                glowBg.alpha = 0.5f
            }
            HudState.ERROR -> {
                image.alpha = 0.7f
                glowBg.setColorFilter(android.graphics.Color.parseColor("#FF5252"), PorterDuff.Mode.SRC_ATOP)
                glowBg.alpha = 0.55f
            }
            HudState.SPEAKING -> {
                // Bright white-cyan
                image.alpha = 1f
                glowBg.setColorFilter(Color.parseColor("#E0FFFF"), PorterDuff.Mode.SRC_ATOP)
                startBreathing(0.65f, 1.0f, 750)
                softScale(1.05f, 1.22f)
            }
        }
    }

    private fun startBreathing(minA: Float, maxA: Float, durationMs: Long) {
        glowBg.alpha = maxA
        alphaAnim = ObjectAnimator.ofFloat(glowBg, "alpha", minA, maxA).apply {
            duration = durationMs
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        glowAnim = ObjectAnimator.ofFloat(image, "alpha", 0.90f, 1.0f).apply {
            duration = durationMs + 80
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun softScale(img: Float, glow: Float) {
        image.animate().scaleX(img).scaleY(img).setDuration(320).start()
        glowBg.animate().scaleX(glow).scaleY(glow).setDuration(320).start()
    }

    override fun onDetachedFromWindow() {
        glowAnim?.cancel()
        alphaAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
