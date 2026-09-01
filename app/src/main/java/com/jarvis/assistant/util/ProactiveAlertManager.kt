package com.jarvis.assistant.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log

class ProactiveAlertManager(
    private val context: Context,
    private val onAlert: (String) -> Unit
) {
    private val settings = SettingsManager(context)
    private val calendar = CalendarHelper(context)
    private var batteryReceiver: BroadcastReceiver? = null
    private var lastBatteryAlertLevel = -1
    private val alertedEventKeys = mutableSetOf<String>()

    fun start() {
        if (!settings.getProactiveAlertsEnabled()) return
        registerBatteryReceiver()
        checkCalendar()
        Log.i(TAG, "Proactive alerts armed")
    }

    fun stop() {
        try { batteryReceiver?.let { context.unregisterReceiver(it) } } catch (_: Exception) {}
        batteryReceiver = null
    }

    fun tick() {
        if (!settings.getProactiveAlertsEnabled()) return
        checkBatteryLevel()
        checkCalendar()
    }

    private fun registerBatteryReceiver() {
        if (batteryReceiver != null) return
        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_BATTERY_LOW -> speakBattery(getBatteryPct())
                    Intent.ACTION_BATTERY_CHANGED,
                    Intent.ACTION_POWER_CONNECTED,
                    Intent.ACTION_POWER_DISCONNECTED -> checkBatteryLevel()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                context.registerReceiver(batteryReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerReceiver failed", e)
        }
    }

    private fun getBatteryPct(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun checkBatteryLevel() {
        val level = getBatteryPct()
        val threshold = settings.getBatteryAlertThreshold()
        if (isCharging()) {
            if (level > threshold) lastBatteryAlertLevel = -1
            return
        }
        if (level in 1..threshold) {
            val band = if (level <= threshold / 2) 1 else 2
            if (lastBatteryAlertLevel != band) {
                lastBatteryAlertLevel = band
                speakBattery(level)
            }
        } else if (level > threshold) {
            lastBatteryAlertLevel = -1
        }
    }

    private fun speakBattery(level: Int) {
        if (level <= 0) return
        val msg = when {
            level <= 10 -> "Sir, battery is critical at $level percent. Please connect a charger."
            level <= 20 -> "Sir, battery is low at $level percent."
            else -> "Sir, battery is at $level percent."
        }
        onAlert(msg)
    }

    private fun checkCalendar() {
        if (!calendar.hasPermission()) return
        val now = System.currentTimeMillis()
        val windowMs = settings.getCalendarAlertMinutes() * 60_000L
        for (e in calendar.nextEvents(5)) {
            val delta = e.startMs - now
            if (delta < 0 || delta > windowMs) continue
            val key = "\( {e.title}| \){e.startMs}"
            if (key in alertedEventKeys) continue
            alertedEventKeys.add(key)
            if (alertedEventKeys.size > 40) {
                alertedEventKeys.removeAll(alertedEventKeys.take(15).toSet())
            }
            val mins = (delta / 60_000L).toInt().coerceAtLeast(1)
            val loc = e.location?.takeIf { it.isNotBlank() }?.let { " at $it" } ?: ""
            onAlert("Sir, calendar reminder: ${e.title}$loc starts in about $mins minutes.")
        }
    }

    companion object { private const val TAG = "ProactiveAlerts" }
}
