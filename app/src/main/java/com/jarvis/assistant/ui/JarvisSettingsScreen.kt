package com.jarvis.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.ui.theme.HudBg
import com.jarvis.assistant.ui.theme.HudCyan
import com.jarvis.assistant.ui.theme.HudCyan10
import com.jarvis.assistant.ui.theme.HudCyanDim
import com.jarvis.assistant.ui.theme.HudPanel
import com.jarvis.assistant.ui.theme.HudTextDim
import com.jarvis.assistant.ui.theme.HudWhite

enum class VoiceType { MALE, FEMALE, ROBOT }

@Composable
fun JarvisSettingsScreen(
    userName: String = "naveen sir",
    voiceType: VoiceType = VoiceType.MALE,
    voiceSpeed: Float = 1.0f,
    rememberedNote: String = "",
    voiceEnrolled: Boolean = false,
    onSaveName: (String) -> Unit = {},
    onVoiceTypeChange: (VoiceType) -> Unit = {},
    onVoiceSpeedChange: (Float) -> Unit = {},
    onClearNote: () -> Unit = {},
    onEnrollVoice: () -> Unit = {},
    onEnableVoiceLock: () -> Unit = {},
    onResetVoiceEnrollment: () -> Unit = {},
    onOpenLockScreenSettings: () -> Unit = {},
    onUsageStatsClick: () -> Unit = {},
    onSendFeedbackClick: () -> Unit = {},
    onClose: () -> Unit = {}
) {
    var nameField by remember { mutableStateOf(userName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "J.A.R.V.I.S — SETTINGS",
            color = HudCyan,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        NavRow("USAGE STATS", onClick = onUsageStatsClick, topPadding = 18.dp)
        NavRow("SEND FEEDBACK", onClick = onSendFeedbackClick, topPadding = 12.dp)

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Column {
                Text("┃ VOICE TYPE", color = HudCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Current: ${voiceType.name.lowercase()}",
                    color = HudTextDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoiceType.entries.forEach { type ->
                        VoiceOptionCard(
                            label = type.name,
                            selected = type == voiceType,
                            modifier = Modifier.weight(1f),
                            onClick = { onVoiceTypeChange(type) }
                        )
                    }
                }
            }
        }

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column {
                Text("┃ VOICE SPEED", color = HudCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "${"%.1f".format(voiceSpeed)}x",
                    color = HudWhite,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Slider(
                    value = voiceSpeed,
                    onValueChange = onVoiceSpeedChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = HudWhite,
                        activeTrackColor = HudCyan,
                        inactiveTrackColor = HudCyanDim
                    )
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("0.5x", "1.0x", "1.5x", "2.0x").forEachIndexed { i, label ->
                        Text(
                            label,
                            color = HudTextDim,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f),
                            textAlign = when (i) {
                                0 -> androidx.compose.ui.text.style.TextAlign.Start
                                3 -> androidx.compose.ui.text.style.TextAlign.End
                                else -> androidx.compose.ui.text.style.TextAlign.Center
                            }
                        )
                    }
                }
            }
        }

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column {
                Text("┃ YOUR NAME", color = HudCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                TextField(
                    value = nameField,
                    onValueChange = { nameField = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = HudBg,
                        unfocusedContainerColor = HudBg,
                        focusedTextColor = HudWhite,
                        unfocusedTextColor = HudWhite,
                        cursorColor = HudCyan,
                        focusedIndicatorColor = HudCyanDim,
                        unfocusedIndicatorColor = HudCyanDim
                    )
                )
                HudActionButton(
                    text = "💾  SAVE NAME",
                    onClick = { onSaveName(nameField) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column {
                Text("┃ REMEMBERED NOTE", color = HudCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Text(
                    rememberedNote.ifBlank { "(nothing remembered yet)" },
                    color = HudTextDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 4.dp)
                )
                HudActionButton(
                    text = "🗑  CLEAR NOTE",
                    onClick = onClearNote,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column {
                Text("┃ VOICE AUTHENTICATION", color = HudCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Approximate on-device voice matching — not a bank-grade biometric, but good enough to reject an obviously different voice. Checked once per app session, not on every command.",
                    color = HudTextDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    if (voiceEnrolled) "Enrolled." else "Not enrolled yet.",
                    color = HudCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 8.dp)
                )
                HudActionButton(
                    text = "🎤  ENROLL MY VOICE",
                    onClick = onEnrollVoice,
                    emphasized = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
                HudActionButton(
                    text = "🔒  ENABLE VOICE LOCK",
                    onClick = onEnableVoiceLock,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                HudActionButton(
                    text = "🔄  RESET VOICE ENROLLMENT",
                    onClick = onResetVoiceEnrollment,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
        }

        HudPanel(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            Column {
                Text("┃ LOCK SCREEN ACCESS", color = HudCyan, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                Text(
                    "Android no longer allows regular apps to set or change your device's lock screen PIN/pattern directly (a security restriction since Android 8) — only this shortcut into system settings is possible. For an in-app lock instead, use \"lock [app name]\" by voice, which is Jarvis's own PIN-gated app lock.",
                    color = HudTextDim,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = 6.dp)
                )
                HudActionButton(
                    text = "⚙  OPEN LOCK SCREEN SETTINGS",
                    onClick = onOpenLockScreenSettings,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                )
            }
        }

        HudActionButton(
            text = "←  CLOSE",
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 10.dp)
        )
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit, topPadding: androidx.compose.ui.unit.Dp) {
    HudPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                color = HudCyan,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            TextButton(onClick = onClick) {
                Text("›", color = HudCyan, fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun VoiceOptionCard(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (selected) HudCyan10 else androidx.compose.ui.graphics.Color.Transparent
    val border = if (selected) HudCyan else HudCyanDim
    val textColor = if (selected) HudCyan else HudTextDim

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(label, color = textColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
    }
}

@Composable
private fun HudActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false
) {
    val bg = if (emphasized) HudCyan10 else androidx.compose.ui.graphics.Color.Transparent
    val border = if (emphasized) HudCyan else HudCyanDim
    val textColor = if (emphasized) HudCyan else HudCyan

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(if (emphasized) 2.dp else 1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(
            text,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}
