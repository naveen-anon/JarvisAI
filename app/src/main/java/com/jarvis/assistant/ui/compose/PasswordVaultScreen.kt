package com.jarvis.assistant.ui.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.systemBars

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jarvis.assistant.security.PasswordVault

@Composable
fun PasswordVaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val vault = remember { PasswordVault(context) }
    var unlocked by remember { mutableStateOf(vault.isUnlocked()) }
    var hasPin by remember { mutableStateOf(vault.hasMasterPin()) }
    var pin by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf<PasswordVault.Entry>()) }

    fun refreshList() {
        entries = if (vault.isUnlocked()) vault.list() else emptyList()
        unlocked = vault.isUnlocked()
        hasPin = vault.hasMasterPin()
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
                HudTitle("PASSWORD VAULT")
                Text("SECURE · PRIVATE · YOURS", color = JColors.Amber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            GlassButton("LOCK", onClick = {
                vault.lock()
                unlocked = false
                pin = ""
                entries = emptyList()
            })
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "● AES-256  ·  ON-DEVICE ENCRYPTION",
            color = JColors.Cyan,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(12.dp))

        if (!unlocked) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (!hasPin) "CREATE SECURE" else "UNLOCK", color = JColors.Text, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text("VAULT", color = JColors.Cyan, fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (!hasPin) "Set a master PIN (min 4). Encrypts passwords on this device."
                        else "Enter master PIN to decrypt entries.",
                        color = JColors.TextDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(14.dp))
                    GlassCard(corner = 14.dp, modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = pin,
                            onValueChange = { pin = it.filter { c -> c.isDigit() }.take(12) },
                            textStyle = TextStyle(color = JColors.Text, fontSize = 16.sp, fontFamily = FontFamily.Monospace),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            cursorBrush = SolidColor(JColors.Cyan),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            decorationBox = { inner ->
                                if (pin.isEmpty()) {
                                    Text("Master PIN", color = JColors.CyanSoft, fontSize = 15.sp, fontFamily = FontFamily.Monospace)
                                }
                                inner()
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    val score = when {
                        pin.length >= 8 -> 4
                        pin.length >= 6 -> 3
                        pin.length >= 4 -> 2
                        pin.length >= 1 -> 1
                        else -> 0
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("PIN STRENGTH", color = JColors.CyanSoft, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.width(8.dp))
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(4) { i ->
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .background(
                                            if (i < score) JColors.Cyan else JColors.Panel,
                                            RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            listOf("—", "WEAK", "FAIR", "GOOD", "STRONG")[score],
                            color = JColors.Cyan,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (status.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(status, color = JColors.Amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("CLOSE", onClick = onBack, modifier = Modifier.weight(1f))
                        GlassButton(
                            if (!hasPin) "CREATE VAULT" else "UNLOCK",
                            onClick = {
                                if (!hasPin) {
                                    if (vault.setMasterPin(pin)) {
                                        status = "Vault ready."
                                        unlocked = true
                                        refreshList()
                                    } else status = "PIN must be at least 4 digits."
                                } else {
                                    if (vault.unlock(pin)) {
                                        status = ""
                                        unlocked = true
                                        refreshList()
                                    } else status = "Wrong PIN."
                                }
                            },
                            modifier = Modifier.weight(1.2f),
                            filled = true
                        )
                    }
                }
            }
        } else {
            GlassButton("+ ADD ENTRY", onClick = { /* simple add via dialog-less inline */ }, filled = true, modifier = Modifier.fillMaxWidth())
            // Inline add fields
            var title by remember { mutableStateOf("") }
            var user by remember { mutableStateOf("") }
            var pass by remember { mutableStateOf("") }
            Spacer(Modifier.height(10.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    VaultField("Title", title) { title = it }
                    VaultField("Username", user) { user = it }
                    VaultField("Password", pass, password = true) { pass = it }
                    GlassButton("SAVE ENTRY", onClick = {
                        if (title.isNotBlank() && pass.isNotBlank()) {
                            vault.add(title, user, pass)
                            title = ""; user = ""; pass = ""
                            refreshList()
                        }
                    }, filled = true, modifier = Modifier.fillMaxWidth())
                }
            }
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text("Vault empty — add an entry.", color = JColors.CyanSoft, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            } else {
                entries.forEach { e ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(e.title.ifBlank { "(untitled)" }, color = JColors.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            if (e.username.isNotBlank()) {
                                Text(e.username, color = JColors.CyanBright, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            Text("••••••••", color = JColors.CyanSoft, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton("SHOW", onClick = { status = "${e.username} / ${e.password}" })
                                GlassButton("DELETE", onClick = { vault.delete(e.id); refreshList() })
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (status.isNotBlank()) {
                Text(status, color = JColors.Amber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
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
private fun VaultField(hint: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    GlassCard(corner = 12.dp, modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = TextStyle(color = JColors.Text, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
            visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            cursorBrush = SolidColor(JColors.Cyan),
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(hint, color = JColors.CyanSoft, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                inner()
            }
        )
    }
}
