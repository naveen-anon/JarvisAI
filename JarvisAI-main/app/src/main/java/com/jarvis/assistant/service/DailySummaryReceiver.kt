package com.jarvis.assistant.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.util.AutoLearnEngine

/**
 * Phase 5 — "Daily activity summary". Fired once a day by an AlarmManager alarm
 * (scheduled from AssistantForegroundService.onCreate) and posts a notification
 * built from AutoLearnEngine's tracked habits — the same text "my routine" returns
 * when asked by voice, just surfaced proactively instead of on-demand.
 */
class DailySummaryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val summary = AutoLearnEngine(context).getDailyRoutineSummary()

        val notification = NotificationCompat.Builder(context, AssistantForegroundService.CHANNEL_ID)
            .setContentTitle("Jarvis — Daily Summary")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setSmallIcon(R.drawable.ic_mic)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(DAILY_SUMMARY_NOTIF_ID, notification)
    }

    companion object {
        const val DAILY_SUMMARY_NOTIF_ID = 202
        const val ACTION_DAILY_SUMMARY = "com.jarvis.assistant.DAILY_SUMMARY"
    }
}
