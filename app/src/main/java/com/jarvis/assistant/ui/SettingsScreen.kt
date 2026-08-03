package com.jarvis.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.*

@Composable
fun SettingsScreen(
    userName: String,
    onSaveName: (String) -> Unit,
    onUsageStatsClick: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    onVoiceTypeSelected: (String) -> Unit,
    currentVoiceType: String,
    voiceSpeed: Float,
    onVoiceSpeedChange: (Float) -> Unit,
    onEnrollVoice: () -> Unit,
    onEnableVoiceLock: () -> Unit,
    onResetVoiceEnrollment: () -> Unit,
    voiceEnrolled: Boolean,
    onOpenLockScreenSettings: () -> Unit,
    onClearNote: () -> Unit
) {
    var nameField by remember { mutableStateOf(userName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SectionHeader("J.A.R.V.I.S — SETTINGS")

        Spacer(Modifier.height(16.dp))

        // Usage stats + feedback (linked rows)
        SettingsPanel {
            LinkRow(Icons.Filled.BarChart, "USAGE STATS", "View system usage and activity", onUsageStatsClick)
            Divider(color = BorderCyan, thickness = 1.dp)
            LinkRow(Icons.Filled.ChatBubble, "SEND FEEDBACK", "Help improve J.A.R.V.I.S", onSendFeedbackClick)
        }

        Spacer(Modifier.height(16.dp))

        // Voice type
        SettingsPanel {
            LabelWithBar("VOICE TYPE")
            Text("Current: $currentVoiceType", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("male", "female", "robot").forEach { type ->
                    VoiceTypeChip(
                        label = type.uppercase(),
                        selected = currentVoiceType == type,
                        onClick = { onVoiceTypeSelected(type) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Voice speed
        SettingsPanel {
            LabelWithBar("VOICE SPEED")
            Text("${voiceSpeed}x", color = Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = voiceSpeed,
                onValueChange = onVoiceSpeedChange,
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan,
                    inactiveTrackColor = BorderCyan
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("0.5x", "1.0x", "1.5x", "2.0x").forEach {
                    Text(it, color = TextDim, fontSize = 11.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Your name
        SettingsPanel {
            LabelWithBar("YOUR NAME")
            OutlinedTextField(
                value = nameField,
                onValueChange = { nameField = it },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Cyan,
                    unfocusedBorderColor = BorderCyan,
                    focusedTextColor = TextMain,
                    unfocusedTextColor = TextMain
                ),
                leadingIcon = { Icon(Icons.Filled.Person, null, tint = CyanDim) }
            )
            Spacer(Modifier.height(10.dp))
            HudButton(text = "SAVE NAME", icon = Icons.Filled.Save, onClick = { onSaveName(nameField) })
        }

        Spacer(Modifier.height(16.dp))

        // Remembered note
        SettingsPanel {
            LabelWithBar("REMEMBERED NOTE")
            Text("(nothing remembered yet)", color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            HudButton(text = "CLEAR NOTE", icon = Icons.Filled.Delete, onClick = onClearNote)
        }

        Spacer(Modifier.height(16.dp))

        // Voice authentication
        SettingsPanel {
            LabelWithBar("VOICE AUTHENTICATION")
            Text(
                "Approximate on-device voice matching — not a bank-grade biometric, but good enough to reject an obviously different voice. Checked once per app session, not on every command.",
                color = TextDim,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Text(
                if (voiceEnrolled) "Enrolled." else "Not enrolled yet.",
                color = if (voiceEnrolled) Success else Cyan,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            HudButton(text = "ENROLL MY VOICE", icon = Icons.Filled.Mic, onClick = onEnrollVoice, filled = true)
            Spacer(Modifier.height(10.dp))
            HudButton(text = "ENABLE VOICE LOCK", icon = Icons.Filled.Lock, onClick = onEnableVoiceLock)
            Spacer(Modifier.height(10.dp))
            HudButton(text = "RESET VOICE ENROLLMENT", icon = Icons.Filled.Refresh, onClick = onResetVoiceEnrollment)
        }

        Spacer(Modifier.height(16.dp))

        // Lock screen
        SettingsPanel {
            LabelWithBar("LOCK SCREEN")
            Text(
                "Android no longer allows regular apps to set or change your device's lock screen PIN/pattern directly (a security restriction since Android 8) — only this shortcut into system settings is possible. For an in-app lock instead, use \"lock [app name]\" by voice, which is Jarvis's own PIN-gated app lock.",
                color = TextDim,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            HudButton(text = "OPEN LOCK SCREEN SETTINGS", icon = Icons.Filled.Settings, onClick = onOpenLockScreenSettings)
        }

        Spacer(Modifier.height(30.dp))
    }
}

// ---------- Reusable pieces ----------

@Composable
private fun SectionHeader(title: String) {
    Text(title, color = TextMain, fontSize = 22.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun LabelWithBar(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
        Box(Modifier.width(3.dp).height(16.dp).background(Cyan))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun SettingsPanel(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(14.dp))
            .background(PanelBg, RoundedCornerShape(14.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun LinkRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .border(1.dp, CyanDim, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Cyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextDim, fontSize = 12.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = CyanDim)
    }
}

@Composable
private fun VoiceTypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, if (selected) Cyan else BorderCyan, RoundedCornerShape(12.dp))
            .background(if (selected) Cyan.copy(alpha = 0.12f) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            when (label) {
                "MALE" -> Icons.Filled.Face
                "FEMALE" -> Icons.Filled.Face3
                else -> Icons.Filled.SmartToy
            },
            contentDescription = label,
            tint = if (selected) Cyan else TextDim,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) Cyan else TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HudButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    filled: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Cyan, RoundedCornerShape(10.dp))
            .background(if (filled) Cyan.copy(alpha = 0.18f) else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Cyan, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}
