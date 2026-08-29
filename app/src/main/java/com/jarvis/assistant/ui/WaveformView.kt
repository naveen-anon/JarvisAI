package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.sin
import kotlin.random.Random

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barCount = 32
    private var barHeights = FloatArray(barCount) { 0.12f }
    private var targetHeights = FloatArray(barCount) { 0.12f }
    private var phase = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }

    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00E5FF")
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
        alpha = 40
    }

    var active: Boolean = false
        set(value) {
            field = value
            if (value) {
                animator.duration = 90
                animator.start()
            } else {
                targetHeights = FloatArray(barCount) { i ->
                    0.08f + 0.06f * sin(i * 0.4).toFloat()
                }
            }
        }

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 90
        repeatCount = ValueAnimator.INFINITE
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            phase += 0.08f
            val t = 0.22f
            for (i in 0 until barCount) {
                barHeights[i] = barHeights[i] + (targetHeights[i] - barHeights[i]) * t
            }
            if (active) {
                if (Random.nextFloat() > 0.72f) {
                    for (i in 0 until barCount) {
                        val wave = (sin(phase + i * 0.35) * 0.5 + 0.5).toFloat()
                        targetHeights[i] = 0.18f + wave * 0.7f + Random.nextFloat() * 0.15f
                    }
                }
            } else {
                for (i in 0 until barCount) {
                    targetHeights[i] = 0.08f + 0.05f * (sin(phase * 0.4 + i * 0.35).toFloat() * 0.5f + 0.5f)
                }
            }
            invalidate()
        }
    }

    init {
        animator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) {
            paint.shader = LinearGradient(
                0f, 0f, w.toFloat(), 0f,
                intArrayOf(
                    Color.parseColor("#0B7A94"),
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#5EFFFF"),
                    Color.parseColor("#00E5FF"),
                    Color.parseColor("#0B7A94")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (barCount == 0 || width == 0) return
        val barWidth = width.toFloat() / (barCount * 2.2f)
        val centerY = height / 2f
        paint.strokeWidth = barWidth * 0.85f
        glowPaint.strokeWidth = barWidth * 1.6f

        for (i in 0 until barCount) {
            val x = barWidth * 1.1f + i * barWidth * 2.2f
            val h = (height / 2f) * barHeights[i].coerceIn(0.04f, 1f)
            canvas.drawLine(x, centerY - h, x, centerY + h, glowPaint)
            canvas.drawLine(x, centerY - h, x, centerY + h, paint)
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
