package com.jarvis.assistant.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import com.jarvis.assistant.R

enum class HudState { IDLE, LISTENING, THINKING, SPEAKING }

class ArcReactorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var state: HudState = HudState.IDLE
        set(value) {
            field = value
            applyStateParams()
        }

    private val cyan = Color.parseColor("#00D4FF")
    private val cyanDim = Color.parseColor("#0A6E85")
    private val amber = Color.parseColor("#FFB300")
    private val red = Color.parseColor("#FF3B30")

    private var accentColor = cyan
    private var outerRotation = 0f
    private var midRotation = 0f
    private var tickRotation = 0f
    private var pulseScale = 1f
    private var pulseAlpha = 1f

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coreGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val outerAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 8000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { outerRotation = it.animatedValue as Float; invalidate() }
    }
    private val midAnimator = ValueAnimator.ofFloat(360f, 0f).apply {
        duration = 5000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { midRotation = it.animatedValue as Float; invalidate() }
    }
    private val tickAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 20000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener { tickRotation = it.animatedValue as Float; invalidate() }
    }
    private val pulseAnimator = ValueAnimator.ofFloat(0.85f, 1.15f).apply {
        duration = 1200
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener {
            pulseScale = it.animatedValue as Float
            pulseAlpha = 0.6f + (pulseScale - 0.85f) / 0.30f * 0.4f
            invalidate()
        }
    }

    init {
        outerAnimator.start()
        midAnimator.start()
        tickAnimator.start()
        pulseAnimator.start()
    }

    private fun applyStateParams() {
        when (state) {
            HudState.IDLE -> {
                accentColor = cyanDim
                outerAnimator.duration = 12000
                midAnimator.duration = 8000
                pulseAnimator.duration = 2000
            }
            HudState.LISTENING -> {
                accentColor = cyan
                outerAnimator.duration = 3000
                midAnimator.duration = 2200
                pulseAnimator.duration = 500
            }
            HudState.THINKING -> {
                accentColor = amber
                outerAnimator.duration = 1200
                midAnimator.duration = 900
                pulseAnimator.duration = 350
            }
            HudState.SPEAKING -> {
                accentColor = cyan
                outerAnimator.duration = 2000
                midAnimator.duration = 1500
                pulseAnimator.duration = 700
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = minOf(width, height) / 2f * 0.92f

        drawTickRing(canvas, cx, cy, maxRadius)
        drawDashedRing(canvas, cx, cy, maxRadius * 0.78f, outerRotation, 14, 10f, 4.5f)
        drawDashedRing(canvas, cx, cy, maxRadius * 0.60f, midRotation, 9, 20f, 3.5f)
        drawCore(canvas, cx, cy, maxRadius * 0.34f)
    }

    private fun drawTickRing(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        tickPaint.color = accentColor
        tickPaint.alpha = 70
        tickPaint.strokeWidth = 2f
        val tickCount = 60
        canvas.save()
        canvas.rotate(tickRotation, cx, cy)
        for (i in 0 until tickCount) {
            val angle = Math.toRadians((360.0 / tickCount) * i)
            val len = if (i % 5 == 0) 14f else 7f
            val startR = radius
            val endR = radius - len
            val x1 = cx + (startR * Math.cos(angle)).toFloat()
            val y1 = cy + (startR * Math.sin(angle)).toFloat()
            val x2 = cx + (endR * Math.cos(angle)).toFloat()
            val y2 = cy + (endR * Math.sin(angle)).toFloat()
            canvas.drawLine(x1, y1, x2, y2, tickPaint)
        }
        canvas.restore()
    }

    private fun drawDashedRing(
        canvas: Canvas, cx: Float, cy: Float, radius: Float,
        rotation: Float, segments: Int, strokeWidth: Float, gapDegrees: Float
    ) {
        ringPaint.color = accentColor
        ringPaint.strokeWidth = strokeWidth
        ringPaint.alpha = 220
        val sweepPerSegment = 360f / segments - gapDegrees
        val rect = android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        for (i in 0 until segments) {
            val startAngle = rotation + i * (360f / segments)
            canvas.drawArc(rect, startAngle, sweepPerSegment, false, ringPaint)
        }
    }

    private fun drawCore(canvas: Canvas, cx: Float, cy: Float, baseRadius: Float) {
        val r = baseRadius * pulseScale

        coreGlowPaint.shader = RadialGradient(
            cx, cy, r * 2.2f,
            intArrayOf(
                Color.argb((pulseAlpha * 90).toInt(), Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)),
                Color.TRANSPARENT
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r * 2.2f, coreGlowPaint)

        corePaint.style = Paint.Style.STROKE
        corePaint.strokeWidth = 3f
        corePaint.color = accentColor
        corePaint.alpha = 255
        canvas.drawCircle(cx, cy, r, corePaint)

        corePaint.style = Paint.Style.FILL
        corePaint.alpha = (pulseAlpha * 140).toInt()
        canvas.drawCircle(cx, cy, r * 0.55f, corePaint)
    }

    override fun onDetachedFromWindow() {
        outerAnimator.cancel()
        midAnimator.cancel()
        tickAnimator.cancel()
        pulseAnimator.cancel()
        super.onDetachedFromWindow()
    }
}
