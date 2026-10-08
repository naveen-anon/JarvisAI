package com.jarvis.assistant.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars

import android.provider.AlarmClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.util.JarvisAlarmScheduler
import kotlinx.coroutines.delay

@Composable
fun ClockScreen(onBack: () -> Unit, initialTab: Int = 0) {
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(initialTab) }
    var hour by remember { mutableIntStateOf(7) }
    var minute by remember { mutableIntStateOf(0) }
    var isPm by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    var timerMin by remember { mutableIntStateOf(5) }
    var timerSec by remember { mutableIntStateOf(0) }
    var timerLeft by remember { mutableLongStateOf(0L) }
    var timerRunning by remember { mutableStateOf(false) }

    var swMs by remember { mutableLongStateOf(0L) }
    var swRunning by remember { mutableStateOf(false) }

    LaunchedEffect(timerRunning, timerLeft) {
        if (timerRunning && timerLeft > 0) {
            delay(200)
            timerLeft = (timerLeft - 200).coerceAtLeast(0)
            if (timerLeft == 0L) {
                timerRunning = false
                status = JarvisAlarmScheduler.scheduleInApp(
                    context,
                    java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                    java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE),
                    "Jarvis Timer"
                )
                // fire immediately via ringtone
                try {
                    context.sendBroadcast(
                        android.content.Intent(JarvisAlarmScheduler.ACTION_FIRE)
                            .setPackage(context.packageName)
                            .putExtra(JarvisAlarmScheduler.EXTRA_LABEL, "Timer done")
                    )
                    status = "Timer finished — ringtone + vibrate."
                } catch (_: Exception) {}
            }
        }
    }
    LaunchedEffect(swRunning) {
        while (swRunning) {
            delay(50)
            swMs += 50
        }
    }

    fun setAlarm() {
        var h = hour % 12
        if (isPm) h += 12
        if (hour == 12 && !isPm) h = 0
        if (hour == 12 && isPm) h = 12
        // normalize from 1-12 UI
        val hour24 = when {
            !isPm && hour == 12 -> 0
            !isPm -> hour
            isPm && hour == 12 -> 12
            else -> hour + 12
        }
        status = JarvisAlarmScheduler.setSystemAlarm(context, hour24, minute, "Jarvis")
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(bgBrush())
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("←", color = JColors.Cyan, fontSize = 22.sp, modifier = Modifier.clickable(onClick = onBack).padding(8.dp))
            Column(Modifier.weight(1f)) {
                HudTitle("CLOCK")
                Text("J.A.R.V.I.S", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            GlassCard(corner = 12.dp) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("● ONLINE", color = JColors.Green, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Standing by", color = JColors.CyanSoft, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ALARM", "TIMER", "STOPWATCH").forEachIndexed { i, label ->
                GlassButton(label, onClick = { tab = i }, modifier = Modifier.weight(1f), filled = tab == i)
            }
        }
        Spacer(Modifier.height(14.dp))

        when (tab) {
            0 -> GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔔  SET ALARM", color = JColors.Cyan, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text("Uses phone Clock app when available · else in-app ringtone + vibrate.", color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            GlassButton("AM", onClick = { isPm = false }, filled = !isPm, modifier = Modifier.width(56.dp))
                            Spacer(Modifier.height(6.dp))
                            GlassButton("PM", onClick = { isPm = true }, filled = isPm, modifier = Modifier.width(56.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Stepper(hour, { hour = if (hour >= 12) 1 else hour + 1 }, { hour = if (hour <= 1) 12 else hour - 1 })
                        Text(" : ", color = JColors.Cyan, fontSize = 28.sp, fontFamily = FontFamily.Monospace)
                        Stepper(minute, { minute = (minute + 1) % 60 }, { minute = if (minute == 0) 59 else minute - 1 }, pad = true)
                    }
                    Spacer(Modifier.height(16.dp))
                    GlassButton("🔔  SET ALARM  ›", onClick = { setAlarm() }, filled = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    GlassButton(
                        "⚡  IN-APP ONLY (ringtone + vibrate)",
                        onClick = {
                            val hour24 = when {
                                !isPm && hour == 12 -> 0
                                !isPm -> hour
                                isPm && hour == 12 -> 12
                                else -> hour + 12
                            }
                            status = JarvisAlarmScheduler.scheduleInApp(context, hour24, minute, "Jarvis")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            1 -> GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏲  TIMER", color = JColors.Cyan, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    val display = if (timerLeft > 0) formatTimer(timerLeft) else "%02d:%02d".format(timerMin, timerSec)
                    Text(display, color = JColors.Text, fontSize = 36.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("−1m", onClick = { timerMin = (timerMin - 1).coerceAtLeast(0) })
                        GlassButton("+1m", onClick = { timerMin += 1 })
                        GlassButton("+30s", onClick = {
                            timerSec += 30
                            if (timerSec >= 60) { timerMin++; timerSec -= 60 }
                        })
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("START", onClick = {
                            if (timerLeft <= 0) timerLeft = (timerMin * 60L + timerSec) * 1000L
                            timerRunning = true
                        }, modifier = Modifier.weight(1f), filled = true)
                        GlassButton("PAUSE", onClick = { timerRunning = false }, modifier = Modifier.weight(1f))
                        GlassButton("RESET", onClick = { timerRunning = false; timerLeft = 0L }, modifier = Modifier.weight(1f))
                    }
                }
            }
            else -> GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏱  STOPWATCH", color = JColors.Cyan, fontSize = 16.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(formatSw(swMs), color = JColors.Text, fontSize = 36.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton(if (swRunning) "PAUSE" else "START", onClick = { swRunning = !swRunning }, modifier = Modifier.weight(1f), filled = true)
                        GlassButton("RESET", onClick = { swRunning = false; swMs = 0L }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (status.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(status, color = JColors.Amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Voice: set alarm 7:00 AM · timer 5 minutes · start stopwatch",
            color = JColors.Muted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun Stepper(value: Int, up: () -> Unit, down: () -> Unit, pad: Boolean = false) {
    GlassCard(corner = 14.dp) {
        Column(Modifier.width(88.dp).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("▲", color = JColors.Cyan, fontSize = 14.sp, modifier = Modifier.clickable(onClick = up).padding(4.dp))
            Text("%02d".format(value), color = JColors.Text, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text("▼", color = JColors.Cyan, fontSize = 14.sp, modifier = Modifier.clickable(onClick = down).padding(4.dp))
        }
    }
}

private fun formatTimer(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}

private fun formatSw(ms: Long): String {
    val totalSec = ms / 1000
    val cs = (ms % 1000) / 10
    return "%02d:%02d.%02d".format(totalSec / 60, totalSec % 60, cs)
}
