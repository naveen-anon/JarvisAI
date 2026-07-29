package com.jarvis.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jarvis.assistant.R

/**
 * Phase 5 — "PC se connect" sibling feature: a one-tap home screen widget so Jarvis can be
 * triggered without ever opening the app. Tapping it starts the assistant service (if not
 * already running) and immediately begins a listening cycle.
 */
class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

        val actionIntent = Intent(context, JarvisWidgetActionReceiver::class.java).apply {
            action = JarvisWidgetActionReceiver.ACTION_START_LISTENING
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetMicButton, pendingIntent)

        manager.updateAppWidget(widgetId, views)
    }
}
