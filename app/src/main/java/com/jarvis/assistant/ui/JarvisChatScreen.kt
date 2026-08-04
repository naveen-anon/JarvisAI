package com.jarvis.assistant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.jarvis.assistant.ui.theme.HudCyanFaint
import com.jarvis.assistant.ui.theme.HudGreenOnline
import com.jarvis.assistant.ui.theme.HudTextDim
import com.jarvis.assistant.ui.theme.HudWhite


@Composable
fun JarvisChatScreen(
    messages: List<ChatMessage> = listOf(
        ChatMessage("Jarvis online. Type anything – offline commands work here too.", isUser = false)
    ),
    onSend: (String) -> Unit = {},
    onClose: () -> Unit = {}
) {
    var draft by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HudBg)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(HudCyanFaint)
                    .border(1.dp, HudCyan, CircleShape)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text("J.A.R.V.I.S", color = HudCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("AI ASSISTANT", color = HudTextDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            TextButton(onClick = onClose) {
                Text("← CLOSE", color = HudCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Divider(color = HudCyanDim, thickness = 1.dp, modifier = Modifier.padding(top = 12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("┃ CHAT", color = HudCyan, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
            Text("●  ONLINE", color = HudGreenOnline, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(msg)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...", color = HudTextDim, fontFamily = FontFamily.Monospace) },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = HudCyanFaint,
                    unfocusedContainerColor = HudCyanFaint,
                    focusedTextColor = HudWhite,
                    unfocusedTextColor = HudWhite,
                    cursorColor = HudCyan,
                    focusedIndicatorColor = HudCyanDim,
                    unfocusedIndicatorColor = HudCyanDim
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onSend(draft)
                        draft = ""
                    }
                }
            ) {
                Text("SEND", color = HudCyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val bg = if (message.isUser) HudCyan10 else HudCyanFaint
    val border = if (message.isUser) HudCyan else HudCyanDim
    val textColor = if (message.isUser) HudWhite else HudCyan

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(bg)
                .border(1.dp, border, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(message.text, color = textColor, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        }
    }
}
