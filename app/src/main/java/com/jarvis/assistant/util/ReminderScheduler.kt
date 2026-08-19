package com.jarvis.assistant.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

class ReminderScheduler(private val context: Context) {

    fun scheduleInMinutes(minutes: Int, message: String): String {
        if (minutes <= 0) return "Please specify a valid time, sir."
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        val id = nextId.incrementAndGet()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_MESSAGE, message)
            putExtra(EXTRA_ID, id)
        }
        val pi = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            val note = message.ifBlank { "your reminder" }
            "Understood, sir. I'll remind you about $note in $minutes minutes."
        } catch (_: SecurityException) {
            "Alarm permission is restricted, sir. Allow exact alarms in system settings."
        } catch (_: Exception) {
            "I couldn't set that reminder, sir."
        }
    }

    companion object {
        const val ACTION_FIRE = "com.jarvis.assistant.REMINDER_FIRE"
        const val EXTRA_MESSAGE = "msg"
        const val EXTRA_ID = "id"
        const val CHANNEL_ID = "jarvis_reminders"
        private val nextId = AtomicInteger(3000)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderScheduler.ACTION_FIRE) return
        val msg = intent.getStringExtra(ReminderScheduler.EXTRA_MESSAGE) ?: "Reminder"
        val id = intent.getIntExtra(ReminderScheduler.EXTRA_ID, 3001)
        val mgr = NotificationManagerCompat.from(context)
        val ch = NotificationChannelCompat.Builder(
            ReminderScheduler.CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH
        ).setName("Jarvis Reminders").build()
        mgr.createNotificationChannel(ch)
        val notif = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("J.A.R.V.I.S. Reminder")
            .setContentText(msg)
            .setStyle(NotificationCompat.BigTextStyle().bigText(msg))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            mgr.notify(id, notif)
        } catch (_: SecurityException) {
            Toast.makeText(context, "Jarvis: $msg", Toast.LENGTH_LONG).show()
        }
    }
}
