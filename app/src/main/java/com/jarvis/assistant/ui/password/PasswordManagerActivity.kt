package com.jarvis.assistant.ui.password

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.security.PasswordVault

class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var vault: PasswordVault
    private lateinit var listHost: LinearLayout
    private lateinit var status: TextView
    private lateinit var overlay: FrameLayout
    private lateinit var overlayCard: LinearLayout
    private lateinit var overlayTitle: TextView
    private lateinit var overlayMsg: TextView
    private lateinit var overlayInput: EditText
    private lateinit var overlayBtnOk: TextView
    private lateinit var overlayBtnCancel: TextView
    private lateinit var addFields: LinearLayout

    private var overlayMode: String = "unlock"
    private var addTitle: EditText? = null
    private var addUser: EditText? = null
    private var addPass: EditText? = null
    private var addNotes: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_manager)
        vault = PasswordVault(this)

        findViewById<TextView>(R.id.btnPwmBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnPwmAdd).setOnClickListener {
            if (ensureUnlocked()) showAddGlass()
        }
        findViewById<TextView>(R.id.btnPwmLock).setOnClickListener {
            vault.lock()
            refresh()
            toast("Vault locked.")
        }
        listHost = findViewById(R.id.pwmList)
        status = findViewById(R.id.txtPwmStatus)
        overlay = findViewById(R.id.pwmOverlay)
        overlayCard = findViewById(R.id.pwmOverlayCard)
        overlayTitle = findViewById(R.id.pwmOverlayTitle)
        overlayMsg = findViewById(R.id.pwmOverlayMsg)
        overlayInput = findViewById(R.id.pwmOverlayInput)
        overlayBtnOk = findViewById(R.id.pwmOverlayOk)
        overlayBtnCancel = findViewById(R.id.pwmOverlayCancel)
        addFields = findViewById(R.id.pwmAddFields)

        overlayCard.background = glassPanel()
        styleGlassInput(overlayInput)
        overlayBtnOk.setOnClickListener { onOverlayOk() }
        overlayBtnCancel.setOnClickListener {
            if (overlayMode == "setup" || (overlayMode == "unlock" && !vault.isUnlocked())) {
                finish()
            } else {
                hideOverlay()
            }
        }

        when {
            !vault.hasMasterPin() -> showSetupGlass()
            !vault.isUnlocked() -> showUnlockGlass()
            else -> refresh()
        }
    }

    private fun glassPanel(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = 28f
        setColor(0xCC0A1825.toInt())
        setStroke(2, 0x9900D9FF.toInt())
    }

    private fun styleGlassInput(et: EditText) {
        et.background = GradientDrawable().apply {
            cornerRadius = 18f
            setColor(0x66051018.toInt())
            setStroke(1, 0x6600D9FF.toInt())
        }
        et.setTextColor(Color.parseColor("#E8FBFF"))
        et.setHintTextColor(Color.parseColor("#5A8A99"))
        et.typeface = Typeface.MONOSPACE
        et.setPadding(36, 28, 36, 28)
    }

    private fun ensureUnlocked(): Boolean {
        if (vault.isUnlocked()) return true
        if (!vault.hasMasterPin()) { showSetupGlass(); return false }
        showUnlockGlass()
        return false
    }

    private fun showOverlay() { overlay.visibility = View.VISIBLE }
    private fun hideOverlay() {
        overlay.visibility = View.GONE
        addFields.visibility = View.GONE
        overlayInput.visibility = View.VISIBLE
        overlayMsg.visibility = View.VISIBLE
        overlayBtnOk.setOnClickListener { onOverlayOk() }
    }

    private fun showSetupGlass() {
        overlayMode = "setup"
        overlayTitle.text = "CREATE VAULT"
        overlayMsg.text = "Set a master PIN (min 4). Encrypts passwords on this device."
        overlayInput.hint = "Master PIN"
        overlayInput.setText("")
        overlayInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        overlayBtnOk.text = "SAVE"
        overlayBtnCancel.text = "CLOSE"
        addFields.visibility = View.GONE
        overlayInput.visibility = View.VISIBLE
        showOverlay()
    }

    private fun showUnlockGlass() {
        overlayMode = "unlock"
        overlayTitle.text = "UNLOCK VAULT"
        overlayMsg.text = "Enter master PIN to decrypt entries."
        overlayInput.hint = "Master PIN"
        overlayInput.setText("")
        overlayInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        overlayBtnOk.text = "UNLOCK"
        overlayBtnCancel.text = "CLOSE"
        addFields.visibility = View.GONE
        overlayInput.visibility = View.VISIBLE
        showOverlay()
    }

    private fun showAddGlass() {
        overlayMode = "add"
        overlayTitle.text = "ADD ENTRY"
        overlayMsg.text = "Stored encrypted until you lock the vault."
        overlayInput.visibility = View.GONE
        addFields.visibility = View.VISIBLE
        addFields.removeAllViews()
        addTitle = glassField("Title (e.g. Gmail)").also { addFields.addView(it) }
        addUser = glassField("Username / email").also { addFields.addView(it) }
        addPass = glassField("Password", true).also { addFields.addView(it) }
        addNotes = glassField("Notes (optional)").also { addFields.addView(it) }
        overlayBtnOk.text = "SAVE"
        overlayBtnCancel.text = "CANCEL"
        showOverlay()
    }

    private fun glassField(hint: String, password: Boolean = false): EditText {
        return EditText(this).apply {
            this.hint = hint
            inputType = if (password)
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            else InputType.TYPE_CLASS_TEXT
            styleGlassInput(this)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }
    }

    private fun onOverlayOk() {
        when (overlayMode) {
            "setup" -> {
                if (vault.setMasterPin(overlayInput.text.toString())) {
                    hideOverlay(); toast("Vault ready."); refresh()
                } else toast("PIN must be at least 4 digits.")
            }
            "unlock" -> {
                if (vault.unlock(overlayInput.text.toString())) {
                    hideOverlay(); refresh()
                } else toast("Wrong PIN.")
            }
            "add" -> {
                val title = addTitle?.text?.toString().orEmpty()
                val pass = addPass?.text?.toString().orEmpty()
                if (title.isBlank() || pass.isBlank()) {
                    toast("Title and password required."); return
                }
                if (vault.add(title, addUser?.text?.toString().orEmpty(), pass,
                        addNotes?.text?.toString().orEmpty())) {
                    hideOverlay(); toast("Saved."); refresh()
                } else toast("Could not save.")
            }
            "reveal" -> hideOverlay()
        }
    }

    private fun refresh() {
        listHost.removeAllViews()
        if (!vault.isUnlocked()) {
            status.text = "LOCKED — enter master PIN"
            status.setTextColor(Color.parseColor("#FFAA00"))
            return
        }
        val entries = vault.list()
        status.text = if (entries.isEmpty()) "Vault empty — tap ADD ENTRY"
        else "${entries.size} entr${if (entries.size == 1) "y" else "ies"}"
        status.setTextColor(Color.parseColor("#5A8A99"))

        entries.forEach { e ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(28, 24, 28, 24)
                background = glassPanel()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 14 }
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
                overlayMode = "reveal"
                overlayTitle.text = e.title.ifBlank { "ENTRY" }
                overlayMsg.text = buildString {
                    if (e.username.isNotBlank()) append("User: ${e.username}\n")
                    append("Pass: ${e.password}")
                    if (e.notes.isNotBlank()) append("\n${e.notes}")
                }
                overlayInput.visibility = View.GONE
                addFields.visibility = View.GONE
                overlayBtnOk.text = "OK"
                overlayBtnCancel.text = "CLOSE"
                showOverlay()
            })
            row.addView(chip("COPY") {
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText("password", e.password))
                toast("Copied.")
            })
            row.addView(chip("DELETE") {
                vault.delete(e.id); refresh(); toast("Deleted.")
            })
            card.addView(row)
            listHost.addView(card)
        }
    }

    private fun chip(label: String, onClick: () -> Unit): TextView =
        TextView(this).apply {
            text = label
            setTextColor(Color.parseColor("#00D9FF"))
            textSize = 11f
            typeface = Typeface.MONOSPACE
            gravity = Gravity.CENTER
            setPadding(22, 14, 22, 14)
            background = GradientDrawable().apply {
                cornerRadius = 16f
                setColor(0x440A1825.toInt())
                setStroke(1, 0x8800D9FF.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 10 }
            setOnClickListener { onClick() }
        }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
