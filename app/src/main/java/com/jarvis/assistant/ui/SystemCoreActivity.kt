package com.jarvis.assistant.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.ui.briefing.BriefingActivity

class SystemCoreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_core)

        findViewById<TextView>(R.id.btnCoreBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnCoreBriefing).setOnClickListener {
            startActivity(Intent(this, BriefingActivity::class.java))
        }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val bat = batteryPct()
        findViewById<ProgressBar>(R.id.barBattery).progress = bat
        findViewById<TextView>(R.id.txtBatteryPct).text = "$bat%"

        val mem = memoryPct()
        findViewById<ProgressBar>(R.id.barMemory).progress = mem
        findViewById<TextView>(R.id.txtMemoryPct).text = "$mem%"

        val stor = storagePct()
        findViewById<ProgressBar>(R.id.barStorage).progress = stor
        findViewById<TextView>(R.id.txtStoragePct).text = "$stor%"

        findViewById<TextView>(R.id.txtNetworkStatus).text = networkLabel()
        findViewById<TextView>(R.id.txtEnvironment).text = "Local"
        findViewById<TextView>(R.id.txtActiveTasks).text = "0"
    }

    private fun batteryPct(): Int {
        val s = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0
        val level = s.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = s.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        return (level * 100) / scale
    }

    private fun memoryPct(): Int {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        if (info.totalMem <= 0L) return 0
        val used = info.totalMem - info.availMem
        return ((used * 100) / info.totalMem).toInt().coerceIn(0, 100)
    }

    private fun storagePct(): Int {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val total = stat.totalBytes
            val avail = stat.availableBytes
            if (total <= 0L) 0
            else (((total - avail) * 100) / total).toInt().coerceIn(0, 100)
        } catch (_: Exception) {
            0
        }
    }

    private fun networkLabel(): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val n = cm.activeNetwork ?: return "OFFLINE"
            val caps = cm.getNetworkCapabilities(n) ?: return "OFFLINE"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE DATA"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
                else -> "ONLINE"
            }
        } catch (_: Exception) {
            "UNKNOWN"
        }
    }
}
