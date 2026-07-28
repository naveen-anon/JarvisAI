package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

class HudOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#0A6E85")
        alpha = 25
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val bracketPaint = Paint().apply {
        color = Color.parseColor("#00D4FF")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val particlePaint = Paint().apply {
        color = Color.parseColor("#00D4FF")
        style = Paint.Style.FILL
    }

    private data class Particle(var x: Float, var y: Float, var speed: Float, var radius: Float, var alpha: Int)

    private val particles = mutableListOf<Particle>()
    private var initialized = false
    private var animTime = 0f

    private val bracketLen = 60f
    private val bracketMargin = 24f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!initialized && w > 0 && h > 0) {
            repeat(25) {
                particles.add(
                    Particle(
                        x = Random.nextFloat() * w,
                        y = Random.nextFloat() * h,
                        speed = 0.15f + Random.nextFloat() * 0.3f,
                        radius = 1f + Random.nextFloat() * 2f,
                        alpha = 20 + Random.nextInt(60)
                    )
                )
            }
            initialized = true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        drawCornerBrackets(canvas)
        drawParticles(canvas)

        animTime += 0.02f
        for (p in particles) {
            p.y -= p.speed
            if (p.y < -10) {
                p.y = height.toFloat() + 10
                p.x = Random.nextFloat() * width
            }
        }
        postInvalidateOnAnimation()
    }

    private fun drawGrid(canvas: Canvas) {
        val step = 80f
        var x = 0f
        while (x < width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y < height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    private fun drawCornerBrackets(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val m = bracketMargin
        val len = bracketLen

        canvas.drawLine(m, m, m + len, m, bracketPaint)
        canvas.drawLine(m, m, m, m + len, bracketPaint)

        canvas.drawLine(w - m, m, w - m - len, m, bracketPaint)
        canvas.drawLine(w - m, m, w - m, m + len, bracketPaint)

        canvas.drawLine(m, h - m, m + len, h - m, bracketPaint)
        canvas.drawLine(m, h - m, m, h - m - len, bracketPaint)

        canvas.drawLine(w - m, h - m, w - m - len, h - m, bracketPaint)
        canvas.drawLine(w - m, h - m, w - m, h - m - len, bracketPaint)
    }

    private fun drawParticles(canvas: Canvas) {
        for (p in particles) {
            val flicker = (sin(animTime + p.x) * 15).toInt()
            particlePaint.alpha = (p.alpha + flicker).coerceIn(10, 90)
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }
    }
}
