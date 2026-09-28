package com.jarvis.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.RemoteViews
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.util.NetworkStatusManager

class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

            // Battery
            val battPct = try {
                val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                val status = context.registerReceiver(null, ifilter)
                val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = (status?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100).coerceAtLeast(1)
                if (level >= 0) (level * 100) / scale else -1
            } catch (_: Exception) { -1 }

            views.setTextViewText(
                R.id.widgetBatt,
                if (battPct >= 0) "BATT ${battPct}%" else "BATT —%"
            )

            // Network
            val netLabel = try {
                if (NetworkStatusManager(context).isOnline()) "NET ONLINE" else "NET OFFLINE"
            } catch (_: Exception) {
                "NET —"
            }
            views.setTextViewText(R.id.widgetNet, netLabel)

            // Status line
            val status = when {
                battPct in 0..15 -> "POWER LOW"
                netLabel.contains("OFFLINE") -> "LOCAL ONLY"
                else -> "NOMINAL"
            }
            views.setTextViewText(R.id.widgetStatus, status)
            views.setTextViewText(R.id.widgetRam, "SYS  READY")
            views.setTextViewText(R.id.widgetHint, "TAP TO LISTEN")

            // Listen action
            val listenIntent = Intent(context, JarvisWidgetActionReceiver::class.java).apply {
                action = JarvisWidgetActionReceiver.ACTION_START_LISTENING
            }
            val listenPi = PendingIntent.getBroadcast(
                context, 0, listenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetMicButton, listenPi)
            views.setOnClickPendingIntent(R.id.widgetRoot, listenPi)
            views.setOnClickPendingIntent(R.id.widgetHint, listenPi)

            // Title → open app
            val openApp = Intent(context, MainActivity::class.java)
            val openPi = PendingIntent.getActivity(
                context, 1, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetTitle, openPi)

            manager.updateAppWidget(widgetId, views)
        }
    }
}
