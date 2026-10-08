package com.jarvis.assistant.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import java.util.Calendar

/**
 * Primary: system desk clock via AlarmClock intent (survives process death).
 * Secondary: in-app RTC alarm with ringtone + vibration if system UI unavailable.
 */
object JarvisAlarmScheduler {

    const val ACTION_FIRE = "com.jarvis.assistant.ACTION_JARVIS_ALARM"
    const val EXTRA_LABEL = "label"

    fun setSystemAlarm(context: Context, hour24: Int, minute: Int, label: String = "Jarvis"): String {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour24)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            putExtra(AlarmClock.EXTRA_VIBRATE, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
                val ampm = if (hour24 < 12) "AM" else "PM"
                "Opening system clock — alarm $h12:${"%02d".format(minute)} $ampm."
            } else {
                scheduleInApp(context, hour24, minute, label)
            }
        } catch (_: Exception) {
            scheduleInApp(context, hour24, minute, label)
        }
    }

    fun scheduleInApp(context: Context, hour24: Int, minute: Int, label: String = "Jarvis"): String {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            (hour24 * 60 + minute),
            Intent(ACTION_FIRE).setPackage(context.packageName).putExtra(EXTRA_LABEL, label),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
            val h12 = if (hour24 % 12 == 0) 12 else hour24 % 12
            val ampm = if (hour24 < 12) "AM" else "PM"
            return "In-app alarm set for $h12:${"%02d".format(minute)} $ampm (ringtone + vibrate)."
        } catch (_: SecurityException) {
            return "Alarm permission restricted — allow exact alarms in system settings."
        } catch (e: Exception) {
            return "Could not schedule alarm: ${e.message}"
        }
    }
}

class JarvisAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != JarvisAlarmScheduler.ACTION_FIRE) return
        // Ringtone
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ring = RingtoneManager.getRingtone(context, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ring?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            ring?.play()
            // stop after ~45s
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try { ring?.stop() } catch (_: Exception) {}
            }, 45_000L)
        } catch (_: Exception) {}
        // Vibrate
        try {
            val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(pattern, -1)
                }
            }
        } catch (_: Exception) {}
    }
}
