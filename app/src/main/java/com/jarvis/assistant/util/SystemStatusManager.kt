package com.jarvis.assistant.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SystemStatusManager(
    private val context: Context,
    private val onClockUpdate: (String) -> Unit,
    private val onBatteryUpdate: (Int) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var running = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                onBatteryUpdate((level * 100) / scale)
            }
        }
    }

    private val clockTicker = object : Runnable {
        override fun run() {
            onClockUpdate(clockFormat.format(Date()))
            if (running) handler.postDelayed(this, 1000)
        }
    }

    fun start() {
        running = true
        handler.post(clockTicker)
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    fun stop() {
        running = false
        handler.removeCallbacks(clockTicker)
        try { context.unregisterReceiver(batteryReceiver) } catch (e: Exception) {}
    }
}
