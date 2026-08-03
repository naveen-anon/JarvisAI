package com.jarvis.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Color palette matching the HUD theme ----------
private val BgDeep = Color(0xFF02040A)
private val PanelBg = Color(0xFF0B1520)
private val BorderCyan = Color(0xFF16303D)
private val Cyan = Color(0xFF00D4FF)
private val CyanDim = Color(0xFF4A8A9A)
private val TextMain = Color(0xFFEAF6FA)
private val TextDim = Color(0xFF7FA3AD)
private val Success = Color(0xFF00FF88)

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onSend: (String) -> Unit,
    onClose: () -> Unit,
    onMicClick: () -> Unit
) {
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDeep)
    ) {
        // ---------- Header ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(Cyan, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "CHAT",
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .border(1.dp, Success.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(Success, RoundedCornerShape(4.dp))
                )
                Spacer(Modifier.width(6.dp))
                Text("ONLINE", color = Success, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Divider(color = BorderCyan, thickness = 1.dp)

        // ---------- Message list ----------
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { msg ->
                if (msg.isUser) UserBubble(msg.text) else JarvisBubble(msg.text)
            }
        }

        // ---------- Input bar ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, BorderCyan, RoundedCornerShape(24.dp))
                    .background(PanelBg, RoundedCornerShape(24.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onMicClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Mic, contentDescription = "Voice input", tint = Cyan)
                }
                Spacer(Modifier.width(6.dp))
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("Type a message...", color = TextDim) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextMain,
                        unfocusedTextColor = TextMain,
                        cursorColor = Cyan
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .border(1.dp, Cyan, RoundedCornerShape(12.dp))
                    .background(Cyan.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .clickable {
                        if (input.isNotBlank()) {
                            onSend(input)
                            input = ""
                        }
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text("SEND", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun JarvisBubble(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        ReactorAvatar()
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .border(1.dp, BorderCyan, RoundedCornerShape(16.dp))
                .background(PanelBg, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(text, color = Cyan, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, BorderCyan, RoundedCornerShape(16.dp))
                .background(PanelBg, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(text, color = TextMain, fontSize = 14.sp)
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(CyanDim.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ReactorAvatar() {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, Cyan.copy(alpha = 0.6f), RoundedCornerShape(50))
            .background(
                Brush.radialGradient(listOf(Cyan.copy(alpha = 0.5f), BgDeep))
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(50))
                .background(Cyan)
        )
    }
}

