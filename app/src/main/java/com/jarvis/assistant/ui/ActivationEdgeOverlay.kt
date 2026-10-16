package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.min
import kotlin.math.sin

/**
 * Liquid-glass multi-colour transparent edge while Jarvis is listening.
 */
object ActivationEdgeOverlay {

    @Volatile
    private var view: EdgeView? = null

    private val main = Handler(Looper.getMainLooper())

    fun show(context: Context) {
        main.post {
            try {
                val app = context.applicationContext
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    !Settings.canDrawOverlays(app)
                ) {
                    return@post
                }
                if (view != null) {
                    view?.active = true
                    view?.invalidate()
                    return@post
                }
                val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val v = EdgeView(app)
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    android.graphics.PixelFormat.TRANSLUCENT
                )
                lp.gravity = Gravity.TOP or Gravity.START
                wm.addView(v, lp)
                view = v
                v.active = true
            } catch (_: Exception) {
                view = null
            }
        }
    }

    fun hide() {
        main.post {
            try {
                val v = view ?: return@post
                v.active = false
                val wm = v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                try { wm.removeView(v) } catch (_: Exception) {}
                view = null
            } catch (_: Exception) {
                view = null
            }
        }
    }

    private class EdgeView(context: Context) : View(context) {
        var active: Boolean = true
        private var phase = 0f
        private var t = 0f
        private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 22f
            strokeCap = Paint.Cap.ROUND
        }
        private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.2f
            strokeCap = Paint.Cap.ROUND
        }
        private val rect = RectF()
        private val matrix = Matrix()

        // Same palette, transparent liquid-glass alphas baked into ARGB
        private val colors = intArrayOf(
            0x9900D9FF.toInt(),
            0x887B2FFF.toInt(),
            0x99FF2D95.toInt(),
            0x8800FFC6.toInt(),
            0x993B82F6.toInt(),
            0x9900D9FF.toInt()
        )
        private val colorsBright = intArrayOf(
            0xCC00D9FF.toInt(),
            0xBB7B2FFF.toInt(),
            0xCCFF2D95.toInt(),
            0xBB00FFC6.toInt(),
            0xCC3B82F6.toInt(),
            0xCC00D9FF.toInt()
        )

        override fun onDraw(canvas: Canvas) {
            if (!active) return
            t += 0.04f
            val pulse = (0.65f + 0.2f * sin(t.toDouble()).toFloat())
            val inset = 12f
            rect.set(inset, inset, width - inset, height - inset)
            val cx = width / 2f
            val cy = height / 2f
            val shader = SweepGradient(cx, cy, colors, null)
            matrix.setRotate(phase, cx, cy)
            shader.setLocalMatrix(matrix)
            soft.shader = shader
            soft.alpha = (70 * pulse).toInt().coerceIn(40, 100)
            val bright = SweepGradient(cx, cy, colorsBright, null)
            bright.setLocalMatrix(matrix)
            edge.shader = bright
            edge.alpha = (160 * pulse).toInt().coerceIn(100, 200)
            val radius = min(40f, min(width, height) / 16f)
            canvas.drawRoundRect(rect, radius, radius, soft)
            canvas.drawRoundRect(rect, radius, radius, edge)
            phase = (phase + 3.8f) % 360f
            postInvalidateOnAnimation()
        }
    }
}
