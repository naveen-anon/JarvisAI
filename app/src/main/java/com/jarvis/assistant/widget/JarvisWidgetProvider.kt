package com.jarvis.assistant.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.jarvis.assistant.MainActivity
import com.jarvis.assistant.R

class JarvisWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) updateWidget(context, appWidgetManager, id)
    }

    private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_jarvis)

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

        val openApp = Intent(context, MainActivity::class.java)
        val openPi = PendingIntent.getActivity(
            context, 1, openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widgetTitle, openPi)

        manager.updateAppWidget(widgetId, views)
    }
}
