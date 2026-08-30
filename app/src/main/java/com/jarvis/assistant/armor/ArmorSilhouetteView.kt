package com.jarvis.assistant.armor

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

class ArmorSilhouetteView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var primaryColor: Int = Color.parseColor("#C41E3A")
        set(value) { field = value; invalidate() }
    var secondaryColor: Int = Color.parseColor("#F5C518")
        set(value) { field = value; invalidate() }

    private var animTime = 0f

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val reactorGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val reactorCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val visorPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 4000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            animTime = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        animator.start()
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        val cx = w / 2f
        val bob = sin(animTime * 2f * Math.PI).toFloat() * (h * 0.012f)
        val sway = sin(animTime * 2f * Math.PI + 1f).toFloat() * (w * 0.01f)
        val reactorPulse = 0.6f + 0.4f * ((sin(animTime * 2f * Math.PI * 2f) + 1f) / 2f)

        canvas.save()
        canvas.translate(sway, bob)

        backGlowPaint.shader = RadialGradient(
            cx, h * 0.42f, w * 0.55f,
            intArrayOf(colorWithAlpha(primaryColor, 40), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, h * 0.42f, w * 0.55f, backGlowPaint)

        bodyPaint.color = primaryColor
        edgePaint.color = colorWithAlpha(secondaryColor, 220)

        val headR = w * 0.11f
        val headCy = h * 0.14f
        val shoulderY = h * 0.24f
        val shoulderW = w * 0.34f
        val torsoBottomY = h * 0.56f
        val torsoBottomW = w * 0.24f
        val hipY = h * 0.60f
        val legBottomY = h * 0.97f
        val armTopW = w * 0.09f
        val armBottomW = w * 0.075f
        val armBottomY = h * 0.52f

        canvas.drawCircle(cx, headCy, headR, bodyPaint)
        canvas.drawCircle(cx, headCy, headR, edgePaint)

        visorPaint.color = colorWithAlpha(Color.parseColor("#00E5FF"), (140 + reactorPulse * 100).toInt())
        canvas.drawRect(cx - headR * 0.55f, headCy - headR * 0.12f, cx + headR * 0.55f, headCy + headR * 0.18f, visorPaint)

        val torsoPath = Path().apply {
            moveTo(cx - shoulderW / 2, shoulderY)
            lineTo(cx + shoulderW / 2, shoulderY)
            lineTo(cx + torsoBottomW / 2, torsoBottomY)
            lineTo(cx - torsoBottomW / 2, torsoBottomY)
            close()
        }
        canvas.drawPath(torsoPath, bodyPaint)
        canvas.drawPath(torsoPath, edgePaint)

        canvas.drawRect(cx - torsoBottomW / 2, torsoBottomY, cx + torsoBottomW / 2, hipY, bodyPaint)

        val legGap = w * 0.02f
        canvas.drawRoundRect(cx - torsoBottomW / 2, hipY, cx - legGap, legBottomY, 10f, 10f, bodyPaint)
        canvas.drawRoundRect(cx + legGap, hipY, cx + torsoBottomW / 2, legBottomY, 10f, 10f, bodyPaint)
        canvas.drawRoundRect(cx - torsoBottomW / 2, hipY, cx - legGap, legBottomY, 10f, 10f, edgePaint)
        canvas.drawRoundRect(cx + legGap, hipY, cx + torsoBottomW / 2, legBottomY, 10f, 10f, edgePaint)

        val leftArm = Path().apply {
            moveTo(cx - shoulderW / 2, shoulderY)
            lineTo(cx - shoulderW / 2 - armTopW, shoulderY + h * 0.02f)
            lineTo(cx - shoulderW / 2 - armBottomW, armBottomY)
            lineTo(cx - shoulderW / 2 + w * 0.02f, armBottomY)
            close()
        }
        val rightArm = Path().apply {
            moveTo(cx + shoulderW / 2, shoulderY)
            lineTo(cx + shoulderW / 2 + armTopW, shoulderY + h * 0.02f)
            lineTo(cx + shoulderW / 2 + armBottomW, armBottomY)
            lineTo(cx + shoulderW / 2 - w * 0.02f, armBottomY)
            close()
        }
        canvas.drawPath(leftArm, bodyPaint)
        canvas.drawPath(rightArm, bodyPaint)
        canvas.drawPath(leftArm, edgePaint)
        canvas.drawPath(rightArm, edgePaint)

        val reactorCx = cx
        val reactorCy = shoulderY + (torsoBottomY - shoulderY) * 0.32f
        val reactorR = w * 0.045f
        reactorGlowPaint.shader = RadialGradient(
            reactorCx, reactorCy, reactorR * (2.2f + reactorPulse),
            intArrayOf(colorWithAlpha(Color.parseColor("#00E5FF"), (180 * reactorPulse).toInt()), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(reactorCx, reactorCy, reactorR * (2.2f + reactorPulse), reactorGlowPaint)
        canvas.drawCircle(reactorCx, reactorCy, reactorR * 0.5f, reactorCorePaint)

        canvas.restore()
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }
}
