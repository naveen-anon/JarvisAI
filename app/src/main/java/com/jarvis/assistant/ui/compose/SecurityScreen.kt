package com.jarvis.assistant.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.jarvis.assistant.util.SecurityAdvisor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SecurityScreen(
    onBack: () -> Unit,
    onSpeak: (String) -> Unit
) {
    val context = LocalContext.current
    var report by remember { mutableStateOf<SecurityAdvisor.Report?>(null) }
    var loading by remember { mutableStateOf(true) }

    suspend fun scan() {
        loading = true
        report = withContext(Dispatchers.IO) {
            try { SecurityAdvisor(context).assess() } catch (_: Exception) { null }
        }
        loading = false
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
            Text("←", color = JColors.Cyan, fontSize = 22.sp, modifier = Modifier
                .clickable(onClick = onBack)
                .padding(8.dp))
            Column(Modifier.weight(1f)) {
                HudTitle("SECURITY")
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

        val r = report
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .background(JColors.Panel)
                        .border(2.dp, JColors.Cyan.copy(alpha = 0.6f), RoundedCornerShape(48.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (loading) "…" else "${r?.score ?: "—"}",
                            color = JColors.Cyan,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text("/100", color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("SECURITY POSTURE", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    Text(
                        "Grade ${r?.grade ?: "—"}",
                        color = JColors.Text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text("Score ${r?.score ?: "—"}", color = JColors.Cyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    val posture = when {
                        r == null -> "Scanning…"
                        r.score >= 85 -> "Security posture is strong, sir."
                        r.score >= 70 -> "Security posture is fair, sir."
                        else -> "Security posture needs attention, sir."
                    }
                    Text(posture, color = JColors.TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (r != null) {
            val crit = r.findings.count { it.severity == SecurityAdvisor.Severity.CRITICAL }
            val high = r.findings.count { it.severity == SecurityAdvisor.Severity.HIGH }
            val med = r.findings.count { it.severity == SecurityAdvisor.Severity.MEDIUM }
            val low = r.findings.count { it.severity == SecurityAdvisor.Severity.LOW }
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("🛡  RISK SUMMARY", color = JColors.Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text("● Critical  $crit", color = JColors.Red, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text("● High  $high", color = JColors.Amber, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Text("● Medium  $med", color = Color(0xFFE8C547), fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                        Text("● Low  $low", color = JColors.Cyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            val issues = r.findings.filter { it.severity != SecurityAdvisor.Severity.OK }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("⚠  PRIORITY FINDINGS", color = JColors.Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text("${issues.size} ISSUES", color = JColors.CyanSoft, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(8.dp))

            if (issues.isEmpty()) {
                Text("No priority issues — controls look healthy.", color = JColors.CyanSoft, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } else {
                issues.forEach { f ->
                    FindingCard(f) {
                        try {
                            val i = when {
                                f.id.contains("notif", true) -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                f.id.contains("a11y", true) || f.id.contains("access", true) -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                f.id.contains("adb", true) -> Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                                f.id.contains("lock", true) || f.id.contains("timeout", true) -> Intent(Settings.ACTION_SECURITY_SETTINGS)
                                else -> Intent(Settings.ACTION_SETTINGS)
                            }
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(i)
                        } catch (_: Exception) {}
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        var rescan by remember { mutableIntStateOf(0) }
        LaunchedEffect(rescan) { scan() }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("REFRESH", onClick = { rescan++ }, modifier = Modifier.weight(1f), filled = false)
            GlassButton("READ ALOUD", onClick = { r?.spoken?.let(onSpeak) }, modifier = Modifier.weight(1f), filled = true)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "—  J.A.R.V.I.S  ·  SECURE YOUR DIGITAL LIFE  —",
            color = JColors.Muted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun FindingCard(f: SecurityAdvisor.Finding, onFix: () -> Unit) {
    val (badge, color) = when (f.severity) {
        SecurityAdvisor.Severity.CRITICAL -> "CRITICAL" to JColors.Red
        SecurityAdvisor.Severity.HIGH -> "HIGH RISK" to JColors.Amber
        SecurityAdvisor.Severity.MEDIUM -> "MEDIUM RISK" to Color(0xFFE8C547)
        SecurityAdvisor.Severity.LOW -> "LOW RISK" to JColors.Cyan
        else -> "OK" to JColors.CyanSoft
    }
    GlassCard(borderColor = color.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(f.title, color = JColors.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.weight(1f))
                Text("⚠ $badge", color = color, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(f.detail, color = JColors.TextDim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            f.action?.let {
                Spacer(Modifier.height(4.dp))
                Text("→ $it", color = JColors.CyanSoft, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Spacer(Modifier.height(10.dp))
            GlassButton(
                if (f.severity == SecurityAdvisor.Severity.HIGH || f.severity == SecurityAdvisor.Severity.CRITICAL) "FIX  ›" else "REVIEW  ›",
                onClick = onFix,
                filled = false
            )
        }
    }
}
