package com.jarvis.assistant.ui.compose

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun SystemCoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var uptime by remember { mutableStateOf("—") }
    var model by remember { mutableStateOf("—") }
    var bat by remember { mutableIntStateOf(0) }
    var mem by remember { mutableIntStateOf(0) }
    var stor by remember { mutableIntStateOf(0) }
    var charge by remember { mutableStateOf("—") }
    var temp by remember { mutableStateOf("—") }
    var volt by remember { mutableStateOf("—") }
    var health by remember { mutableStateOf("—") }
    var net by remember { mutableStateOf("—") }
    var ip by remember { mutableStateOf("—") }
    var link by remember { mutableStateOf("—") }
    var cpu by remember { mutableStateOf("—") }
    var platform by remember { mutableStateOf("—") }
    var diag by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            bat = batteryPct(context)
            mem = memoryPct(context)
            stor = storagePct()
            uptime = formatUptime(SystemClock.elapsedRealtime())
            model = "${Build.MANUFACTURER.uppercase(Locale.US)} ${Build.MODEL}"
            val st = batteryStatus(context)
            charge = st.first
            temp = st.second
            volt = st.third
            health = if (bat >= 20) "GOOD" else "LOW"
            net = networkLabel(context)
            ip = localIpv4()
            link = wifiLinkSpeed(context)
            cpu = "arm${if (Build.SUPPORTED_ABIS.isNotEmpty()) Build.SUPPORTED_ABIS[0] else ""} (${Runtime.getRuntime().availableProcessors()} CORES)"
            platform = "ANDROID ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            val headroom = mi.availMem / (1024 * 1024)
            diag = buildString {
                appendLine("[SYS_OK] JARVIS NEURAL BUS ONLINE")
                appendLine("[POWER] ARC REACTOR $volt NOMINAL")
                appendLine("[MEM] HEADROOM: ${headroom} MB")
                appendLine("[COMMS] IP UPLINK: $ip · $net")
            }.trim()
            delay(1000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(bgBrush())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = JColors.Cyan, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
            Column(Modifier.weight(1f)) {
                HudTitle("SYSTEM CORE")
                Text("SYSTEM DIAGNOSTICS", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Text("● LIVE", color = JColors.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("CORE UPTIME", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text(uptime, color = JColors.Text, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(6.dp))
                Text(model, color = JColors.CyanBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("NEURAL PROTOCOL: ONLINE", color = JColors.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricBar("POWER", bat, JColors.Cyan)
                MetricBar("MEMORY", mem, Color(0xFFA78BFA))
                MetricBar("STORAGE", stor, JColors.Amber)
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ARC REACTOR POWER MATRIX", color = JColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(charge, color = JColors.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatCell("CELL TEMP", temp, Modifier.weight(1f))
                    StatCell("VOLTAGE", volt, Modifier.weight(1f))
                    StatCell("HEALTH", health, Modifier.weight(1f), valueColor = JColors.Green)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("COMMS AND NETWORK UPLINK", color = JColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Text(net, color = JColors.Green, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row {
                    StatCell("LOCAL IPV4", ip, Modifier.weight(1f))
                    StatCell("LINK", link, Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("NEURAL PROCESSOR ARCHITECTURE", color = JColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Text("CPU CORES  $cpu", color = JColors.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("RUNTIME  $platform", color = JColors.Text, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(Modifier.height(10.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = JColors.Green.copy(alpha = 0.4f)) {
            Column(Modifier.padding(16.dp)) {
                Text("DIAGNOSTIC STREAM", color = JColors.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.height(8.dp))
                Text(diag, color = JColors.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 18.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "—  J.A.R.V.I.S  ·  SYSTEM DIAGNOSTICS  —",
            color = JColors.Muted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun MetricBar(label: String, pct: Int, color: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$pct%", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(label, color = color.copy(alpha = 0.85f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { pct / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = JColors.Panel
        )
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color = JColors.Text) {
    Column(modifier) {
        Text(label, color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

private fun formatUptime(ms: Long): String {
    val h = TimeUnit.MILLISECONDS.toHours(ms)
    val m = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return "${h}h ${m}m ${"%02d".format(s)}s"
}

private fun batteryPct(context: Context): Int {
    val s = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return 0
    val level = s.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
    val scale = s.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    return (level * 100 / scale).coerceIn(0, 100)
}

private fun batteryStatus(context: Context): Triple<String, String, String> {
    val s = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return Triple("—", "—", "—")
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
    return Triple(label, String.format(Locale.US, "%.1f °C", temp), String.format(Locale.US, "%.2f V", volt))
}

private fun memoryPct(context: Context): Int {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    am.getMemoryInfo(info)
    if (info.totalMem <= 0L) return 0
    val used = info.totalMem - info.availMem
    return ((used * 100) / info.totalMem).toInt().coerceIn(0, 100)
}

private fun storagePct(): Int = try {
    val path = Environment.getDataDirectory()
    val stat = StatFs(path.path)
    val total = stat.totalBytes
    val avail = stat.availableBytes
    if (total <= 0L) 0 else (((total - avail) * 100) / total).toInt().coerceIn(0, 100)
} catch (_: Exception) { 0 }

private fun networkLabel(context: Context): String = try {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val n = cm.activeNetwork ?: return "OFFLINE"
    val caps = cm.getNetworkCapabilities(n) ?: return "OFFLINE"
    when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WI-FI (ONLINE)"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE DATA (ONLINE)"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET (ONLINE)"
        else -> "ONLINE"
    }
} catch (_: Exception) { "UNKNOWN" }

private fun localIpv4(): String = try {
    NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }
        .firstOrNull { !it.isLoopbackAddress && it is Inet4Address }?.hostAddress ?: "—"
} catch (_: Exception) { "—" }

@Suppress("DEPRECATION")
private fun wifiLinkSpeed(context: Context): String = try {
    val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val speed = wm.connectionInfo.linkSpeed
    if (speed > 0) "$speed Mbps" else "—"
} catch (_: Exception) { "—" }
