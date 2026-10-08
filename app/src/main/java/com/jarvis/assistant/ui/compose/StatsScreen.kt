package com.jarvis.assistant.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.util.AutoLearnEngine

@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val stats = remember { AutoLearnEngine(context).getUsageStats() }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .background(bgBrush())
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(JColors.Panel.copy(alpha = 0.6f), CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text("☰", color = JColors.Cyan, fontSize = 16.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row {
                    Text("USAGE ", color = JColors.Cyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("STATS", color = JColors.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Text("Your Jarvis activity at a glance", color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            GlassCard(corner = 12.dp) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦", color = JColors.Cyan, fontSize = 14.sp)
                    Text("Live", color = JColors.CyanSoft, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("▣", color = JColors.Cyan, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("TOTAL COMMANDS", color = JColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Commands executed", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(10.dp))
                    Text("${stats.totalInteractions}", color = JColors.Cyan, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    MiniBars(stats.totalInteractions)
                }
            }
            GlassCard(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(14.dp)) {
                    Text("🔥", color = JColors.Cyan, fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("DAY STREAK", color = JColors.Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Consecutive days active", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${stats.currentStreak}", color = JColors.Cyan, fontSize = 32.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(" 🔥", color = JColors.Cyan, fontSize = 18.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    StreakDots(stats.currentStreak.coerceAtMost(7))
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Text("APP USAGE", color = JColors.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text("Your most used apps (from Jarvis)", color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(10.dp))

        val appsText = if (stats.topApps.isEmpty())
            "Not enough data yet — keep using Jarvis"
        else stats.topApps.take(3).joinToString(" · ") { "${it.first} (${it.second}x)" }

        ListRow("★", "MOST USED APPS", appsText)
        Spacer(Modifier.height(8.dp))

        val contactsText = if (stats.topContacts.isEmpty())
            "Not enough data yet."
        else stats.topContacts.take(3).joinToString(" · ") { it.first }

        ListRow("👤", "MOST CONTACTED", contactsText)
        Spacer(Modifier.height(12.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("QUICK INSIGHTS", color = JColors.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Spacer(Modifier.height(10.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), borderColor = JColors.Cyan.copy(alpha = 0.3f)) {
                    Text(
                        insightText(stats),
                        color = JColors.TextDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        GlassButton("‹  CLOSE", onClick = onBack, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "Active ${stats.daysActive}d · First use ${stats.firstUsedDaysAgo}d ago",
            color = JColors.Muted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ListRow(icon: String, title: String, subtitle: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(JColors.Panel, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = JColors.Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(subtitle, color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Text("›", color = JColors.Cyan, fontSize = 18.sp)
        }
    }
}

@Composable
private fun MiniBars(seed: Int) {
    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
        val n = 12
        val w = size.width / (n * 1.6f)
        val rnd = java.util.Random((seed + 7).toLong())
        for (i in 0 until n) {
            val h = size.height * (0.25f + rnd.nextFloat() * 0.75f)
            drawLine(
                color = JColors.Cyan.copy(alpha = 0.75f),
                start = Offset(i * w * 1.6f + w / 2f, size.height),
                end = Offset(i * w * 1.6f + w / 2f, size.height - h),
                strokeWidth = w * 0.7f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun StreakDots(filled: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(7) { i ->
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (i < filled) JColors.Cyan else JColors.Panel,
                        CircleShape
                    )
            )
        }
    }
}

private fun insightText(stats: AutoLearnEngine.UsageStats): String {
    return when {
        stats.totalInteractions == 0 ->
            "Keep using Jarvis to unlock detailed insights, app stats, and more!"
        stats.currentStreak >= 3 ->
            "Solid ${stats.currentStreak}-day streak. You've run ${stats.totalInteractions} commands total."
        stats.topApps.isNotEmpty() ->
            "Top app via Jarvis: ${stats.topApps.first().first}. ${stats.totalInteractions} commands logged."
        else ->
            "${stats.totalInteractions} commands across ${stats.daysActive} active days. Keep going."
    }
}
