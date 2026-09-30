package com.jarvis.assistant.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.os.SystemClock
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.TimeUnit

class SystemCoreActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_system_core)
        findViewById<TextView>(R.id.btnCoreBack).setOnClickListener { finish() }
        refresh()
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
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

        findViewById<TextView>(R.id.txtUptime).text = formatUptime(SystemClock.elapsedRealtime())
        findViewById<TextView>(R.id.txtDeviceModel).text =
            "${Build.MANUFACTURER.uppercase(Locale.US)} ${Build.MODEL}"

        val status = batteryStatus()
        findViewById<TextView>(R.id.txtChargeStatus).text = status.first
        findViewById<TextView>(R.id.txtTemp).text = status.second
        findViewById<TextView>(R.id.txtVoltage).text = status.third
        findViewById<TextView>(R.id.txtHealth).text = if (bat >= 20) "GOOD" else "LOW"

        val net = networkLabel()
        findViewById<TextView>(R.id.txtNetworkStatus).text = net
        findViewById<TextView>(R.id.txtIp).text = localIpv4()
        findViewById<TextView>(R.id.txtBandwidth).text = wifiLinkSpeed()

        val cores = Runtime.getRuntime().availableProcessors()
        findViewById<TextView>(R.id.txtCpu).text =
            "CPU CORES  ${Build.SUPPORTED_ABIS.firstOrNull() ?: "ARM"} ($cores CORES)"
        findViewById<TextView>(R.id.txtPlatform).text =
            "RUNTIME  ANDROID ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        val headroomMb = mi.availMem / (1024 * 1024)
        findViewById<TextView>(R.id.txtDiagStream).text = buildString {
            appendLine("[SYS_OK] STARK NEURAL BUS ONLINE")
            appendLine("[POWER] ARC REACTOR ${status.third} NOMINAL")
            appendLine("[MEM] HEADROOM: ${headroomMb} MB")
            appendLine("[COMMS] IP UPLINK: ${localIpv4()} · $net")
        }.trim()
    }

    private fun formatUptime(ms: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(ms)
        val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return String.format(Locale.US, "%dh %02dm %02ds", h, m, s)
    }

    private fun batteryPct(): Int {
        val s = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0
        val level = s.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = s.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        return (level * 100) / scale
    }

    private fun batteryStatus(): Triple<String, String, String> {
        val s = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return Triple("UNKNOWN", "—", "—")
        val st = s.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val label = when (st) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "CHARGING"
            BatteryManager.BATTERY_STATUS_FULL -> "FULL"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "DISCHARGING"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "NOT CHARGING"
            else -> "UNKNOWN"
        }
        val temp = s.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
        val volt = s.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
        return Triple(
            label,
            String.format(Locale.US, "%.1f °C", temp),
            String.format(Locale.US, "%.2f V", volt)
        )
    }

    private fun memoryPct(): Int {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        if (info.totalMem <= 0L) return 0
        return (((info.totalMem - info.availMem) * 100) / info.totalMem).toInt().coerceIn(0, 100)
    }

    private fun storagePct(): Int = try {
        val stat = StatFs(Environment.getDataDirectory().path)
        if (stat.totalBytes <= 0L) 0
        else (((stat.totalBytes - stat.availableBytes) * 100) / stat.totalBytes).toInt().coerceIn(0, 100)
    } catch (_: Exception) { 0 }

    private fun networkLabel(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return "OFFLINE"
        val caps = cm.getNetworkCapabilities(net) ?: return "OFFLINE"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI (ONLINE)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR (ONLINE)"
            else -> "ONLINE"
        }
    }

    private fun localIpv4(): String = try {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { !it.isLoopbackAddress && it is Inet4Address }
            ?.hostAddress ?: "—"
    } catch (_: Exception) { "—" }

    @Suppress("DEPRECATION")
    private fun wifiLinkSpeed(): String = try {
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val spd = wm.connectionInfo.linkSpeed
        if (spd > 0) "$spd Mbps" else "—"
    } catch (_: Exception) { "—" }
}
