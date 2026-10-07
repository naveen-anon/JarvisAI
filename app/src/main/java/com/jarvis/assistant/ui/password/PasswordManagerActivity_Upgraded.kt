package com.jarvis.assistant.ui.password

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.security.PasswordVault

/**
 * J.A.R.V.I.S Password Vault — upgraded HUD version.
 *
 * Drop-in replacement for the original PasswordManagerActivity.
 * Keeps the existing PasswordVault API:
 * hasMasterPin(), isUnlocked(), setMasterPin(), unlock(), lock(),
 * add(), list(), delete().
 *
 * No new XML is required.
 */
class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var vault: PasswordVault
    private lateinit var root: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private lateinit var pinInput: EditText
    private lateinit var primaryButton: TextView
    private lateinit var strengthLabel: TextView
    private lateinit var strengthRow: LinearLayout
    private lateinit var listHost: LinearLayout
    private lateinit var searchInput: EditText

    private var mode = MODE_SETUP

    companion object {
        private const val MODE_SETUP = "setup"
        private const val MODE_UNLOCK = "unlock"
        private const val MODE_LIST = "list"

        private const val BG = 0xFF020812.toInt()
        private const val PANEL = 0xFF061525.toInt()
        private const val PANEL_2 = 0xFF081B2A.toInt()
        private const val CYAN = 0xFF00E5FF.toInt()
        private const val CYAN_DIM = 0xFF0B6175.toInt()
        private const val TEXT = 0xFFD9F7FF.toInt()
        private const val MUTED = 0xFF7399A8.toInt()
        private const val GREEN = 0xFF20E878.toInt()
        private const val ORANGE = 0xFFFFA000.toInt()
        private const val RED = 0xFFFF4D61.toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        vault = PasswordVault(this)
        buildUi()

        when {
            !vault.hasMasterPin() -> showSetupUi()
            !vault.isUnlocked() -> showUnlockUi()
            else -> showListUi()
        }
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }

        root.addView(buildHeader(), lp())

        scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(28))
        }
        scroll.addView(content, lp())
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }

    private fun buildHeader(): View {
        val bar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(8))
        }

        val back = tv("‹", 38f, CYAN).apply {
            setOnClickListener { finish() }
            gravity = Gravity.CENTER
        }
        bar.addView(back, LinearLayout.LayoutParams(dp(46), dp(54)))

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleBox.addView(tv("PASSWORD VAULT", 24f, CYAN, true))
        titleBox.addView(tv("J.A.R.V.I.S  //  ENCRYPTED ON DEVICE", 9f, MUTED))
        bar.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))

        val lock = chip("LOCK", RED)
        lock.setOnClickListener {
            if (vault.isUnlocked()) {
                vault.lock()
                showUnlockUi()
                toast("Vault locked.")
            }
        }
        bar.addView(lock)
        return bar
    }

    private fun showSetupUi() {
        mode = MODE_SETUP
        content.removeAllViews()
        content.addView(hero("VAULT CORE", "INITIALIZATION REQUIRED", CYAN))
        content.addView(space(12))
        content.addView(coreStatus("●", "VAULT OFFLINE", "Create a master PIN to initialize encrypted storage."))
        content.addView(space(14))
        content.addView(buildPinCard("CREATE MASTER PIN", "Minimum 4 digits. Use a PIN you can remember."))
        content.addView(space(16))
        content.addView(footer("LOCAL ENCRYPTION  •  NO CLOUD SYNC"))
    }

    private fun showUnlockUi() {
        mode = MODE_UNLOCK
        content.removeAllViews()
        content.addView(hero("VAULT CORE", "LOCKED", ORANGE))
        content.addView(space(12))
        content.addView(coreStatus("◆", "SECURE LOCK", "Enter your master PIN to unlock encrypted entries."))
        content.addView(space(14))
        content.addView(buildPinCard("UNLOCK VAULT", "The vault remains locked until the correct PIN is supplied."))
        content.addView(space(16))
        content.addView(footer("AUTO-LOCK ACTIVE  •  DEVICE STORAGE"))
    }

    private fun showListUi() {
        mode = MODE_LIST
        content.removeAllViews()

        content.addView(hero("VAULT CORE", "SECURE / ONLINE", GREEN))
        content.addView(space(12))
        content.addView(buildVaultSummary())
        content.addView(space(12))

        val add = primary("＋  ADD NEW ENTRY")
        add.setOnClickListener { showAddDialog() }
        content.addView(add)
        content.addView(space(12))

        searchInput = field("Search vault entries…", false).apply {
            setSingleLine(true)
        }
        content.addView(searchInput)
        content.addView(space(12))

        listHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(listHost)
        refreshList("")
        content.addView(space(12))
        content.addView(footer("AES-STYLE LOCAL VAULT  •  J.A.R.V.I.S"))

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshList(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
    }

    private fun buildPinCard(title: String, subtitle: String): View {
        val box = panel()
        box.addView(tv(title, 18f, TEXT, true))
        box.addView(space(4))
        box.addView(tv(subtitle, 11f, MUTED))
        box.addView(space(14))

        pinInput = field("MASTER PIN", true)
        pinInput.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        pinInput.setSingleLine(true)
        box.addView(pinInput)
        box.addView(space(8))

        strengthRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        box.addView(strengthRow, LinearLayout.LayoutParams(-1, dp(5)))
        box.addView(space(6))
        strengthLabel = tv("—", 10f, MUTED, true)
        box.addView(strengthLabel)
        box.addView(space(14))

        primaryButton = primary(if (mode == MODE_SETUP) "CREATE VAULT" else "UNLOCK VAULT")
        primaryButton.setOnClickListener { onPrimaryAction() }
        box.addView(primaryButton)

        pinInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = updateStrength(s?.toString().orEmpty())
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })
        updateStrength("")
        return box
    }

    private fun onPrimaryAction() {
        val pin = pinInput.text.toString()
        when (mode) {
            MODE_SETUP -> if (vault.setMasterPin(pin)) {
                toast("Vault initialized.")
                showListUi()
            } else toast("PIN must be at least 4 digits.")

            MODE_UNLOCK -> if (vault.unlock(pin)) {
                toast("Vault unlocked.")
                showListUi()
            } else {
                pinInput.setText("")
                toast("Wrong PIN.")
            }
        }
    }

    private fun updateStrength(pin: String) {
        if (!::strengthRow.isInitialized || !::strengthLabel.isInitialized) return
        strengthRow.removeAllViews()
        val score = when {
            pin.length >= 10 -> 4
            pin.length >= 8 -> 3
            pin.length >= 4 -> 2
            pin.isNotEmpty() -> 1
            else -> 0
        }
        val label = when (score) {
            0 -> "ENTER PIN"
            1 -> "WEAK"
            2 -> "FAIR"
            3 -> "GOOD"
            else -> "STRONG"
        }
        strengthLabel.text = label
        strengthLabel.setTextColor(when (score) {
            1 -> RED
            2 -> ORANGE
            else -> if (score >= 3) CYAN else MUTED
        })
        repeat(4) { i ->
            val segment = View(this).apply {
                background = rounded(if (i < score) CYAN else 0xFF102532.toInt(), 4f)
            }
            val p = LinearLayout.LayoutParams(0, -1, 1f)
            if (i < 3) p.marginEnd = dp(6)
            strengthRow.addView(segment, p)
        }
    }

    private fun buildVaultSummary(): View {
        val box = panel()
        val count = vault.list().size
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val core = tv("◉", 36f, CYAN, true)
        row.addView(core, LinearLayout.LayoutParams(dp(58), dp(58)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(tv("VAULT STATUS", 10f, MUTED, true))
        text.addView(tv("ENCRYPTED  •  UNLOCKED", 15f, GREEN, true))
        text.addView(tv("$count secure entr${if (count == 1) "y" else "ies"}", 10f, MUTED))
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(row)
        return box
    }

    private fun refreshList(query: String) {
        if (!::listHost.isInitialized) return
        listHost.removeAllViews()
        val q = query.trim().lowercase()
        val entries = vault.list().filter {
            q.isBlank() || it.title.lowercase().contains(q) || it.username.lowercase().contains(q)
        }

        if (entries.isEmpty()) {
            listHost.addView(panel().apply {
                addView(tv(if (q.isBlank()) "VAULT EMPTY" else "NO MATCHES", 15f, CYAN, true))
                addView(space(4))
                addView(tv(if (q.isBlank()) "Tap ADD NEW ENTRY to begin." else "Try another search term.", 10f, MUTED))
            })
            return
        }

        entries.forEach { entry ->
            listHost.addView(entryCard(entry))
            listHost.addView(space(10))
        }
    }

    private fun entryCard(e: PasswordVault.Entry): View {
        val card = panel()
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(tv("◆", 16f, CYAN, true), LinearLayout.LayoutParams(dp(28), -2))
        titleRow.addView(tv(e.title.ifBlank { "UNTITLED" }, 15f, TEXT, true), LinearLayout.LayoutParams(0, -2, 1f))
        titleRow.addView(tv("SECURE", 8f, GREEN, true))
        card.addView(titleRow)

        if (e.username.isNotBlank()) {
            card.addView(space(5))
            card.addView(tv(e.username, 10f, MUTED))
        }

        card.addView(space(7))
        card.addView(tv("••••••••••••", 12f, CYAN_DIM))
        card.addView(space(10))

        val buttons = LinearLayout(this)
        buttons.addView(chip("SHOW", CYAN).apply { setOnClickListener { showEntry(e) } })
        buttons.addView(chip("COPY", CYAN).apply { setOnClickListener { copyPassword(e.password) } }, LinearLayout.LayoutParams(-2, -2))
        buttons.addView(chip("DELETE", RED).apply {
            setOnClickListener {
                AlertDialog.Builder(this@PasswordManagerActivity)
                    .setTitle("Delete entry?")
                    .setMessage("Remove ${e.title} from the vault?")
                    .setNegativeButton("CANCEL", null)
                    .setPositiveButton("DELETE") { _, _ ->
                        vault.delete(e.id)
                        refreshList(searchInput.text.toString())
                        toast("Entry deleted.")
                    }.show()
            }
        }, LinearLayout.LayoutParams(-2, -2))
        card.addView(buttons)
        return card
    }

    private fun showEntry(e: PasswordVault.Entry) {
        AlertDialog.Builder(this)
            .setTitle("◆  ${e.title}")
            .setMessage("USERNAME\n${e.username.ifBlank { "—" }}\n\nPASSWORD\n${e.password}\n\nNOTES\n${e.notes.ifBlank { "—" }}")
            .setPositiveButton("COPY PASSWORD") { _, _ -> copyPassword(e.password) }
            .setNegativeButton("CLOSE", null)
            .show()
    }

    private fun copyPassword(password: String) {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("password", password))
        toast("Password copied.")
    }

    private fun showAddDialog() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }
        val title = field("Title (e.g. Gmail)")
        val user = field("Username / email")
        val pass = field("Password", true)
        val notes = field("Notes (optional)")
        box.addView(title, fieldLp())
        box.addView(user, fieldLp())
        box.addView(pass, fieldLp())
        box.addView(notes, fieldLp())

        AlertDialog.Builder(this)
            .setTitle("＋  ADD VAULT ENTRY")
            .setView(box)
            .setPositiveButton("SAVE") { _, _ ->
                if (title.text.isBlank() || pass.text.isBlank()) {
                    toast("Title and password required.")
                } else if (vault.add(title.text.toString(), user.text.toString(), pass.text.toString(), notes.text.toString())) {
                    toast("Entry secured.")
                    refreshList(searchInput.text.toString())
                } else toast("Could not save entry.")
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun hero(title: String, state: String, stateColor: Int): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(PANEL_2, 20f, CYAN_DIM)
        }
        box.addView(tv("◉", 30f, CYAN, true), LinearLayout.LayoutParams(dp(54), dp(54)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(tv(title, 17f, TEXT, true))
        text.addView(tv(state, 10f, stateColor, true))
        box.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        return box
    }

    private fun coreStatus(icon: String, title: String, body: String): View {
        val box = panel()
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        row.addView(tv(icon, 23f, CYAN, true), LinearLayout.LayoutParams(dp(42), dp(42)))
        val text = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        text.addView(tv(title, 12f, GREEN, true))
        text.addView(tv(body, 10f, MUTED))
        row.addView(text, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(row)
        return box
    }

    private fun panel(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(PANEL_2, 20f, CYAN_DIM)
    }

    private fun primary(text: String): TextView = tv(text, 13f, BG, true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF008FA8.toInt(), CYAN)).apply {
            cornerRadius = dp(18).toFloat()
        }
    }

    private fun chip(text: String, color: Int): TextView = tv(text, 9f, color, true).apply {
        gravity = Gravity.CENTER
        setPadding(dp(13), dp(9), dp(13), dp(9))
        background = rounded(0x33061A27.toInt(), 13f, color)
        val p = LinearLayout.LayoutParams(-2, -2)
        p.marginEnd = dp(8)
        layoutParams = p
    }

    private fun field(hint: String, password: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint
        setTextColor(TEXT)
        setHintTextColor(MUTED)
        textSize = 13f
        typeface = Typeface.MONOSPACE
        setSingleLine(false)
        inputType = if (password) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        } else InputType.TYPE_CLASS_TEXT
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = rounded(0x55040F19.toInt(), 15f, CYAN_DIM)
    }

    private fun footer(text: String): TextView = tv(text, 9f, MUTED).apply {
        gravity = Gravity.CENTER
        setPadding(0, dp(10), 0, dp(12))
    }

    private fun space(h: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(h)) }

    private fun fieldLp() = LinearLayout.LayoutParams(-1, dp(56)).apply { bottomMargin = dp(10) }
    private fun lp() = LinearLayout.LayoutParams(-1, -2)

    private fun tv(text: String, size: Float, color: Int, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        setTextColor(color)
        textSize = size
        typeface = Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun rounded(fill: Int, radius: Float, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(radius.toInt()).toFloat()
        setColor(fill)
        stroke?.let { setStroke(dp(1), it) }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
    private fun dp(v: Float): Int = (v * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
