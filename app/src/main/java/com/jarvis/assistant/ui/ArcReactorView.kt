package com.jarvis.assistant.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.jarvis.assistant.R

enum class HudState { IDLE, LISTENING, THINKING, SPEAKING }

/**
 * Photoreal arc reactor drawable + HUD states.
 * IDLE dim slow | LISTENING cyan fast | THINKING amber | SPEAKING bright
 */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val image = ImageView(context).apply {
        setImageResource(R.drawable.arc_reactor)
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
    }

    private var rotateAnim: ObjectAnimator? = null
    private var pulseAnim: ObjectAnimator? = null

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyState()
        }

    init {
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyState()
    }

    fun setAccentColor(hex: String) {
        try {
            image.colorFilter = PorterDuffColorFilter(Color.parseColor(hex), PorterDuff.Mode.SRC_ATOP)
        } catch (_: Exception) {
        }
    }

    private fun applyState() {
        rotateAnim?.cancel()
        pulseAnim?.cancel()

        val style = when (state) {
            HudState.IDLE -> Style(0xFF4A90A8.toInt(), 28000L, 0.72f, false)
            HudState.LISTENING -> Style(0xFF00C8E0.toInt(), 8000L, 1f, true)
            HudState.THINKING -> Style(0xFFFFB020.toInt(), 4500L, 1f, true)
            HudState.SPEAKING -> Style(0xFFE8FBFF.toInt(), 6000L, 1f, true)
        }

        image.alpha = style.alpha
        image.colorFilter = PorterDuffColorFilter(style.tint, PorterDuff.Mode.MULTIPLY)

        rotateAnim = ObjectAnimator.ofFloat(image, ROTATION, 0f, 360f).apply {
            duration = style.spinMs
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }

        if (style.pulse) {
            pulseAnim = ObjectAnimator.ofFloat(image, SCALE_X, 0.96f, 1.04f).apply {
                duration = 900
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                addUpdateListener { image.scaleY = image.scaleX }
                start()
            }
        } else {
            image.scaleX = 1f
            image.scaleY = 1f
        }
    }

    private data class Style(
        val tint: Int,
        val spinMs: Long,
        val alpha: Float,
        val pulse: Boolean
    )

    override fun onDetachedFromWindow() {
        rotateAnim?.cancel()
        pulseAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
