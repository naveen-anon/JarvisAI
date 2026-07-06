package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barCount = 24
    private var barHeights = FloatArray(barCount) { 0.15f }
    private var targetHeights = FloatArray(barCount) { 0.15f }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00D4FF")
        strokeCap = Paint.Cap.ROUND
    }

    var active: Boolean = false
        set(value) {
            field = value
            if (value) animator.start() else {
                animator.cancel()
                targetHeights = FloatArray(barCount) { 0.1f }
                barHeights = targetHeights.copyOf()
                invalidate()
            }
        }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 120
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener { frac ->
            val t = frac.animatedValue as Float
            for (i in 0 until barCount) {
                barHeights[i] = lerp(barHeights[i], targetHeights[i], t)
            }
            if (t > 0.95f) {
                targetHeights = FloatArray(barCount) { Random.nextFloat() * 0.85f + 0.15f }
            }
            invalidate()
        }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (barCount == 0) return
        val barWidth = width.toFloat() / (barCount * 2)
        val centerY = height / 2f
        paint.strokeWidth = barWidth * 0.7f

        for (i in 0 until barCount) {
            val x = barWidth + i * barWidth * 2
            val h = (height / 2f) * barHeights[i]
            canvas.drawLine(x, centerY - h, x, centerY + h, paint)
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
