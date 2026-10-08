package com.jarvis.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.widget.RemoteViews
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateOne(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateOne(context: Context, manager: AppWidgetManager, widgetId: Int) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

                val battPct = batteryPct(context)
                views.setTextViewText(
                    R.id.widgetBatt,
                    if (battPct >= 0) "BATT ${battPct}%" else "BATT —%"
                )

                val netLabel = try {
                    @Suppress("DEPRECATION")
                    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val info = cm.activeNetworkInfo
                    if (info != null && info.isConnected) "NET ONLINE" else "NET OFFLINE"
                } catch (_: Exception) {
                    "NET —"
                }
                views.setTextViewText(R.id.widgetNet, netLabel)

                val status = when {
                    battPct in 0..15 -> "POWER LOW"
                    netLabel.contains("OFFLINE") -> "LOCAL ONLY"
                    else -> "STANDING BY"
                }
                views.setTextViewText(R.id.widgetStatus, status)
                views.setTextViewText(R.id.widgetRam, "TAP REACTOR TO LISTEN")
                views.setTextViewText(R.id.widgetHint, "LISTEN")

                // Arc reactor (same visual language as in-app HubReactor)
                val density = context.resources.displayMetrics.density
                val sizePx = (72 * density).toInt().coerceIn(128, 256)
                val reactor = drawArcReactor(sizePx, battPct.coerceIn(0, 100) / 100f)
                views.setImageViewBitmap(R.id.widgetReactor, reactor)

                val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                val listenIntent = Intent(context, JarvisWidgetActionReceiver::class.java).apply {
                    action = JarvisWidgetActionReceiver.ACTION_START_LISTENING
                }
                val listenPi = PendingIntent.getBroadcast(context, 0, listenIntent, flags)
                views.setOnClickPendingIntent(R.id.widgetReactor, listenPi)
                views.setOnClickPendingIntent(R.id.widgetRoot, listenPi)
                views.setOnClickPendingIntent(R.id.widgetHint, listenPi)

                val openPi = PendingIntent.getActivity(
                    context, 1, Intent(context, MainActivity::class.java), flags
                )
                views.setOnClickPendingIntent(R.id.widgetTitle, openPi)

                manager.updateAppWidget(widgetId, views)
            } catch (_: Exception) {
                try {
                    val views = RemoteViews(context.packageName, R.layout.widget_jarvis)
                    views.setTextViewText(R.id.widgetStatus, "TAP TO OPEN")
                    val openPi = PendingIntent.getActivity(
                        context, 1, Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widgetRoot, openPi)
                    manager.updateAppWidget(widgetId, views)
                } catch (_: Exception) {
                }
            }
        }

        private fun batteryPct(context: Context): Int {
            return try {
                val s = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: return -1
                val level = s.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = s.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                (level * 100 / scale).coerceIn(0, 100)
            } catch (_: Exception) {
                -1
            }
        }

        /** Cyan arc-reactor ring + triangle core — matches home HUD. */
        fun drawArcReactor(size: Int, batteryFrac: Float): Bitmap {
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val cx = size / 2f
            val cy = size / 2f
            val r = min(cx, cy) * 0.92f

            val cyan = 0xFF00D9FF.toInt()
            val cyanDim = 0xFF007A99.toInt()
            val core = 0xFFB8ECFF.toInt()

            // soft glow
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = 0x3300D9FF
            }
            c.drawCircle(cx, cy, r * 0.98f, glow)

            // outer ring
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.035f
                color = cyan
            }
            c.drawCircle(cx, cy, r * 0.88f, ring)

            // battery arc
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.045f
                strokeCap = Paint.Cap.ROUND
                color = cyan
            }
            val oval = RectF(cx - r * 0.72f, cy - r * 0.72f, cx + r * 0.72f, cy + r * 0.72f)
            c.drawArc(oval, -90f, 360f * batteryFrac.coerceIn(0.05f, 1f), false, arcPaint)

            // dashed inner ring (ticks)
            val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.012f
                color = cyanDim
            }
            for (i in 0 until 24) {
                val a = Math.toRadians(i * 15.0)
                val r0 = r * 0.58f
                val r1 = r * 0.66f
                c.drawLine(
                    cx + (r0 * cos(a)).toFloat(),
                    cy + (r0 * sin(a)).toFloat(),
                    cx + (r1 * cos(a)).toFloat(),
                    cy + (r1 * sin(a)).toFloat(),
                    tick
                )
            }

            // triangle core
            val triR = r * 0.28f
            val path = android.graphics.Path()
            for (i in 0 until 3) {
                val a = Math.toRadians(-90.0 + i * 120.0)
                val x = cx + (triR * cos(a)).toFloat()
                val y = cy + (triR * sin(a)).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            val triStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.02f
                color = core
            }
            val triFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = 0x4400D9FF
            }
            c.drawPath(path, triFill)
            c.drawPath(path, triStroke)

            // center dot
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = core
            }
            c.drawCircle(cx, cy, r * 0.06f, dot)
            return bmp
        }
    }
}
