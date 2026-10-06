package com.jarvis.assistant.ui.password

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.security.PasswordVault

/**
 * Jarvis-matched password manager window (cyan glass HUD).
 */
class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var vault: PasswordVault
    private lateinit var listHost: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_manager)
        vault = PasswordVault(this)

        findViewById<TextView>(R.id.btnPwmBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnPwmAdd).setOnClickListener { if (ensureUnlocked()) showAddDialog() }
        findViewById<TextView>(R.id.btnPwmLock).setOnClickListener {
            vault.lock()
            refresh()
            toast("Vault locked.")
        }
        listHost = findViewById(R.id.pwmList)
        status = findViewById(R.id.txtPwmStatus)

        if (!vault.hasMasterPin()) {
            promptSetupPin()
        } else if (!vault.isUnlocked()) {
            promptUnlock()
        } else {
            refresh()
        }
    }

    private fun ensureUnlocked(): Boolean {
        if (vault.isUnlocked()) return true
        if (!vault.hasMasterPin()) {
            promptSetupPin()
            return false
        }
        promptUnlock()
        return false
    }

    private fun promptSetupPin() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Master PIN (min 4)"
            setTextColor(Color.parseColor("#E8FBFF"))
            setHintTextColor(Color.parseColor("#5A8A99"))
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Jarvis Vault")
            .setMessage("Set a master PIN to encrypt your passwords on this device.")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Save") { _, _ ->
                val pin = input.text.toString()
                if (vault.setMasterPin(pin)) {
                    toast("Vault ready.")
                    refresh()
                } else {
                    toast("PIN must be at least 4 digits.")
                    promptSetupPin()
                }
            }
            .setNegativeButton("Close") { _, _ -> finish() }
            .show()
    }

    private fun promptUnlock() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "Master PIN"
            setTextColor(Color.parseColor("#E8FBFF"))
            setHintTextColor(Color.parseColor("#5A8A99"))
            setPadding(40, 30, 40, 30)
        }
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Unlock vault")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("Unlock") { _, _ ->
                if (vault.unlock(input.text.toString())) {
                    refresh()
                } else {
                    toast("Wrong PIN.")
                    promptUnlock()
                }
            }
            .setNegativeButton("Close") { _, _ -> finish() }
            .show()
    }

    private fun showAddDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
        }
        fun field(hint: String, password: Boolean = false) = EditText(this).apply {
            this.hint = hint
            setTextColor(Color.parseColor("#E8FBFF"))
            setHintTextColor(Color.parseColor("#5A8A99"))
            inputType = if (password)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else
                InputType.TYPE_CLASS_TEXT
            setPadding(24, 20, 24, 20)
            box.addView(this)
        }
        val title = field("Title (e.g. Gmail)")
        val user = field("Username / email")
        val pass = field("Password", password = true)
        val notes = field("Notes (optional)")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Add entry")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                if (title.text.isBlank() || pass.text.isBlank()) {
                    toast("Title and password required.")
                    return@setPositiveButton
                }
                if (vault.add(title.text.toString(), user.text.toString(), pass.text.toString(), notes.text.toString())) {
                    toast("Saved.")
                    refresh()
                } else toast("Could not save.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refresh() {
        listHost.removeAllViews()
        if (!vault.isUnlocked()) {
            status.text = "LOCKED — enter master PIN"
            status.setTextColor(Color.parseColor("#FFAA00"))
            return
        }
        val entries = vault.list()
        status.text = if (entries.isEmpty()) "Vault empty — tap ADD"
        else "${entries.size} entr${if (entries.size == 1) "y" else "ies"}"
        status.setTextColor(Color.parseColor("#5A8A99"))

        entries.forEach { e ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 24, 28, 24)
                background = getDrawable(R.drawable.glass_card_bg)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 16
                layoutParams = lp
            }
            card.addView(TextView(this).apply {
                text = e.title.ifBlank { "(untitled)" }
                setTextColor(Color.parseColor("#00D9FF"))
                textSize = 14f
                typeface = Typeface.MONOSPACE
                setTypeface(typeface, Typeface.BOLD)
            })
            if (e.username.isNotBlank()) {
                card.addView(TextView(this).apply {
                    text = e.username
                    setTextColor(Color.parseColor("#B8ECFF"))
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                    setPadding(0, 8, 0, 0)
                })
            }
            card.addView(TextView(this).apply {
                text = "••••••••"
                setTextColor(Color.parseColor("#5A8A99"))
                textSize = 12f
                typeface = Typeface.MONOSPACE
                setPadding(0, 6, 0, 0)
            })
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 0)
            }
            row.addView(chip("SHOW") {
                AlertDialog.Builder(this@PasswordManagerActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(e.title)
                    .setMessage("User: ${e.username}\nPass: ${e.password}\n${e.notes}")
                    .setPositiveButton("OK", null)
                    .show()
            })
            row.addView(chip("COPY PASS") {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("password", e.password))
                toast("Password copied.")
            })
            row.addView(chip("DELETE") {
                AlertDialog.Builder(this@PasswordManagerActivity, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle("Delete ${e.title}?")
                    .setPositiveButton("Delete") { _, _ ->
                        vault.delete(e.id)
                        refresh()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            })
            card.addView(row)
            listHost.addView(card)
        }
    }

    private fun chip(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#00D9FF"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(22, 14, 22, 14)
            background = getDrawable(R.drawable.glass_button_bg)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.marginEnd = 12
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
