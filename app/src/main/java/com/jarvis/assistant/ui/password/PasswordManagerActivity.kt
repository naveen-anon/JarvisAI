package com.jarvis.assistant.ui.password

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.jarvis.assistant.R
import com.jarvis.assistant.security.PasswordVault

/**
 * JARVIS HUD-style password vault (reference UI).
 */
class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var vault: PasswordVault
    private lateinit var setupCard: View
    private lateinit var listSection: View
    private lateinit var listHost: LinearLayout
    private lateinit var status: TextView
    private lateinit var pinInput: EditText
    private lateinit var strengthRow: LinearLayout
    private lateinit var strengthLabel: TextView
    private lateinit var btnOk: TextView
    private lateinit var bioSwitch: SwitchCompat

    private var mode: String = "setup" // setup | unlock | list

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_manager)
        vault = PasswordVault(this)

        setupCard = findViewById(R.id.pwmSetupCard)
        listSection = findViewById(R.id.pwmListSection)
        listHost = findViewById(R.id.pwmList)
        status = findViewById(R.id.txtPwmStatus)
        pinInput = findViewById(R.id.pwmOverlayInput)
        strengthRow = findViewById(R.id.pwmStrengthRow)
        strengthLabel = findViewById(R.id.txtPwmStrength)
        btnOk = findViewById(R.id.pwmOverlayOk)
        bioSwitch = findViewById(R.id.switchPwmBio)

        findViewById<TextView>(R.id.btnPwmBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnPwmLock).setOnClickListener {
            vault.lock()
            showLockedUi()
            toast("Vault locked.")
        }
        findViewById<TextView>(R.id.btnPwmAdd).setOnClickListener {
            if (vault.isUnlocked()) showAddInline()
        }

        pinInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateStrength(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnOk.setOnClickListener { onPrimaryAction() }

        // restore bio preference
        val prefs = getSharedPreferences("jarvis_pwm", MODE_PRIVATE)
        bioSwitch.isChecked = prefs.getBoolean("bio_enabled", true)
        bioSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("bio_enabled", checked).apply()
        }

        when {
            !vault.hasMasterPin() -> showSetupUi()
            !vault.isUnlocked() -> showUnlockUi()
            else -> showListUi()
        }
    }

    private fun showSetupUi() {
        mode = "setup"
        setupCard.visibility = View.VISIBLE
        listSection.visibility = View.GONE
        status.text = "Set a master PIN (min 4).\nEncrypts passwords on this device."
        btnOk.text = "🔒  CREATE VAULT"
        pinInput.setText("")
        updateStrength("")
    }

    private fun showUnlockUi() {
        mode = "unlock"
        setupCard.visibility = View.VISIBLE
        listSection.visibility = View.GONE
        status.text = "Enter master PIN to decrypt entries."
        btnOk.text = "🔓  UNLOCK"
        pinInput.setText("")
        updateStrength("")
    }

    private fun showLockedUi() {
        if (vault.hasMasterPin()) showUnlockUi() else showSetupUi()
    }

    private fun showListUi() {
        mode = "list"
        setupCard.visibility = View.GONE
        listSection.visibility = View.VISIBLE
        refreshList()
    }

    private fun onPrimaryAction() {
        val pin = pinInput.text.toString()
        when (mode) {
            "setup" -> {
                if (vault.setMasterPin(pin)) {
                    toast("Vault ready.")
                    showListUi()
                } else toast("PIN must be at least 4 digits.")
            }
            "unlock" -> {
                if (vault.unlock(pin)) {
                    toast("Unlocked.")
                    showListUi()
                } else toast("Wrong PIN.")
            }
        }
    }

    private fun updateStrength(pin: String) {
        strengthRow.removeAllViews()
        val score = when {
            pin.length >= 8 -> 4
            pin.length >= 6 -> 3
            pin.length >= 4 -> 2
            pin.length >= 1 -> 1
            else -> 0
        }
        val label = when (score) {
            0 -> "—"
            1 -> "WEAK"
            2 -> "FAIR"
            3 -> "GOOD"
            else -> "STRONG"
        }
        strengthLabel.text = label
        strengthLabel.setTextColor(
            when (score) {
                0, 1 -> Color.parseColor("#FF6B6B")
                2 -> Color.parseColor("#FFAA00")
                else -> Color.parseColor("#00D9FF")
            }
        )
        for (i in 0 until 4) {
            val seg = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                    marginEnd = if (i < 3) 6 else 0
                }
                background = GradientDrawable().apply {
                    cornerRadius = 4f
                    setColor(
                        if (i < score) Color.parseColor("#00D9FF")
                        else Color.parseColor("#1A3040")
                    )
                }
            }
            strengthRow.addView(seg)
        }
    }

    private fun showAddInline() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 24, 40, 8)
        }
        fun field(hint: String, password: Boolean = false) = EditText(this).apply {
            this.hint = hint
            setTextColor(Color.parseColor("#E8FBFF"))
            setHintTextColor(Color.parseColor("#5A8A99"))
            typeface = Typeface.MONOSPACE
            inputType = if (password)
                android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            else android.text.InputType.TYPE_CLASS_TEXT
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0x330A1825)
                setStroke(1, 0x6600D9FF)
            }
            setPadding(28, 24, 28, 24)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = 12
            layoutParams = lp
            box.addView(this)
        }
        val title = field("Title (e.g. Gmail)")
        val user = field("Username / email")
        val pass = field("Password", true)
        val notes = field("Notes (optional)")
        androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Add entry")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                if (title.text.isBlank() || pass.text.isBlank()) {
                    toast("Title and password required.")
                    return@setPositiveButton
                }
                if (vault.add(title.text.toString(), user.text.toString(), pass.text.toString(), notes.text.toString())) {
                    toast("Saved.")
                    refreshList()
                } else toast("Could not save.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshList() {
        listHost.removeAllViews()
        val entries = vault.list()
        if (entries.isEmpty()) {
            listHost.addView(TextView(this).apply {
                text = "Vault empty — tap ADD ENTRY"
                setTextColor(Color.parseColor("#5A8A99"))
                typeface = Typeface.MONOSPACE
                textSize = 12f
                setPadding(8, 16, 8, 16)
            })
            return
        }
        entries.forEach { e ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 22, 28, 22)
                background = GradientDrawable().apply {
                    cornerRadius = 22f
                    setColor(0xCC0A1825.toInt())
                    setStroke(2, 0x8800D9FF.toInt())
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
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
                    setPadding(0, 6, 0, 0)
                })
            }
            card.addView(TextView(this).apply {
                text = "••••••••"
                setTextColor(Color.parseColor("#5A8A99"))
                textSize = 12f
                typeface = Typeface.MONOSPACE
            })
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 0)
            }
            fun chip(label: String, action: () -> Unit) = TextView(this@PasswordManagerActivity).apply {
                text = label
                setTextColor(Color.parseColor("#00D9FF"))
                textSize = 11f
                typeface = Typeface.MONOSPACE
                gravity = Gravity.CENTER
                setPadding(20, 12, 20, 12)
                background = GradientDrawable().apply {
                    cornerRadius = 14f
                    setColor(0x330A1825)
                    setStroke(1, 0x6600D9FF)
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 10 }
                setOnClickListener { action() }
            }
            row.addView(chip("SHOW") {
                androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                    .setTitle(e.title)
                    .setMessage("User: ${e.username}\nPass: ${e.password}\n${e.notes}")
                    .setPositiveButton("OK", null)
                    .show()
            })
            row.addView(chip("COPY") {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("password", e.password))
                toast("Copied.")
            })
            row.addView(chip("DELETE") {
                vault.delete(e.id)
                refreshList()
                toast("Deleted.")
            })
            card.addView(row)
            listHost.addView(card)
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
