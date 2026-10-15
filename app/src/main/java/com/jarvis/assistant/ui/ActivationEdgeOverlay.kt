package com.jarvis.assistant.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import kotlin.math.min

/**
 * Siri-style multi-colour edge glow around the screen while Jarvis is active (listening).
 * Uses TYPE_APPLICATION_OVERLAY when permission is granted; no-ops otherwise.
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
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            strokeCap = Paint.Cap.ROUND
        }
        private val soft = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 28f
            strokeCap = Paint.Cap.ROUND
        }
        private val rect = RectF()
        private val matrix = Matrix()

        private val colors = intArrayOf(
            0xFF00D9FF.toInt(),
            0xFF7B2FFF.toInt(),
            0xFFFF2D95.toInt(),
            0xFF00FFC6.toInt(),
            0xFF3B82F6.toInt(),
            0xFF00D9FF.toInt()
        )

        override fun onDraw(canvas: Canvas) {
            if (!active) return
            val inset = 10f
            rect.set(inset, inset, width - inset, height - inset)
            val cx = width / 2f
            val cy = height / 2f
            val shader = SweepGradient(cx, cy, colors, null)
            matrix.setRotate(phase, cx, cy)
            shader.setLocalMatrix(matrix)
            soft.shader = shader
            soft.alpha = 90
            stroke.shader = shader
            stroke.alpha = 220
            val radius = min(36f, min(width, height) / 18f)
            canvas.drawRoundRect(rect, radius, radius, soft)
            canvas.drawRoundRect(rect, radius, radius, stroke)
            phase = (phase + 4.5f) % 360f
            postInvalidateOnAnimation()
        }
    }
}
