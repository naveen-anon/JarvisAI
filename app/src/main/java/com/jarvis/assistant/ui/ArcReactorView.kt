package com.jarvis.assistant.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import com.jarvis.assistant.R

enum class HudState { IDLE, LISTENING, THINKING, SPEAKING }

/** Photoreal reactor — NO rotation. States = tint / pulse only. */
class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val image = ImageView(context).apply {
        setImageResource(R.drawable.arc_reactor)
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
        setBackgroundColor(Color.TRANSPARENT)
        rotation = 0f
    }

    private var pulseAnim: ObjectAnimator? = null

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyState()
        }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val s = minOf(view.width, view.height)
                val left = (view.width - s) / 2
                val top = (view.height - s) / 2
                outline.setOval(left, top, left + s, top + s)
            }
        }
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
        pulseAnim?.cancel()
        image.rotation = 0f

        when (state) {
            HudState.IDLE -> {
                image.alpha = 0.88f
                image.colorFilter = null
                image.scaleX = 1f
                image.scaleY = 1f
            }
            HudState.LISTENING -> {
                image.alpha = 1f
                image.colorFilter = null
                startPulse()
            }
            HudState.THINKING -> {
                image.alpha = 1f
                image.colorFilter = PorterDuffColorFilter(0xFFFFB020.toInt(), PorterDuff.Mode.SRC_ATOP)
                startPulse()
            }
            HudState.SPEAKING -> {
                image.alpha = 1f
                image.colorFilter = PorterDuffColorFilter(0xFFB8F4FF.toInt(), PorterDuff.Mode.SRC_ATOP)
                startPulse()
            }
        }
    }

    private fun startPulse() {
        pulseAnim = ObjectAnimator.ofFloat(image, SCALE_X, 0.97f, 1.03f).apply {
            duration = 900
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { image.scaleY = image.scaleX }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        pulseAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
