package com.jarvis.assistant.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import com.jarvis.assistant.R

enum class HudState { IDLE, LISTENING, THINKING, SPEAKING }

class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val image = ImageView(context).apply {
        setImageResource(R.drawable.arc_reactor)
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
        setBackgroundColor(Color.TRANSPARENT)
    }

    private var pulseAnim: ObjectAnimator? = null

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyState()
        }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(image, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        applyState()
    }

    fun setAccentColor(hex: String) { }

    private fun applyState() {
        pulseAnim?.cancel()
        image.rotation = 0f
        image.clearColorFilter()
        when (state) {
            HudState.IDLE -> {
                image.alpha = 0.9f
                image.scaleX = 1f
                image.scaleY = 1f
            }
            HudState.LISTENING, HudState.THINKING, HudState.SPEAKING -> {
                image.alpha = 1f
                pulseAnim = ObjectAnimator.ofFloat(image, SCALE_X, 0.97f, 1.03f).apply {
                    duration = 900
                    repeatCount = ValueAnimator.INFINITE
                    repeatMode = ValueAnimator.REVERSE
                    addUpdateListener { image.scaleY = image.scaleX }
                    start()
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        pulseAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
