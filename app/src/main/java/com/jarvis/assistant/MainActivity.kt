package com.jarvis.assistant

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.brain.BrainState
import com.jarvis.assistant.diagnostics.CrashHandler
import com.jarvis.assistant.service.AssistantForegroundService
import com.jarvis.assistant.ui.ArcReactorView
import com.jarvis.assistant.ui.HudState
import com.jarvis.assistant.ui.Typewriter
import com.jarvis.assistant.ui.WaveformView
import com.jarvis.assistant.util.LocationHelper
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.util.PerformanceMonitor
import com.jarvis.assistant.util.SystemStatusManager
import com.jarvis.assistant.util.WeatherClient
import com.jarvis.assistant.security.UnlockHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AssistantForegroundService.AssistantListener , com.jarvis.assistant.ui.HudController.Listener{

    private var service: AssistantForegroundService? = null
    private var bound = false

    private lateinit var systemStatus: SystemStatusManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var weatherClient: WeatherClient
    private lateinit var networkStatus: NetworkStatusManager
    private lateinit var perfMonitor: PerformanceMonitor
    private lateinit var settings: com.jarvis.assistant.util.SettingsManager
    private val feedbackHandler = Handler(Looper.getMainLooper())
    private var feedbackCheckPosted = false
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private val perfHandler = Handler(Looper.getMainLooper())

    // UI elements the service pushes updates into - previously declared as local vals
    // in onCreate() and thrown away, so nothing the assistant said ever reached the screen.
    private lateinit var arcReactor: ArcReactorView
    private lateinit var waveform: WaveformView
    private lateinit var txtState: TextView
    private lateinit var txtTranscript: TextView
    private lateinit var txtResponse: TextView

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AssistantForegroundService.LocalBinder).getService()
            service?.listener = this@MainActivity
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    private val requiredPermissions = mutableListOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.CAMERA
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[android.Manifest.permission.RECORD_AUDIO] == true) {
            startAssistantService()
        }
        if (results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            fetchLocationAndWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Check first, before anything else has a chance to crash again — if the
        // previous launch died, show exactly why, right here on-screen, no PC/adb needed.
        showLastCrashIfAny()

        arcReactor = findViewById(R.id.arcReactor)
        waveform = findViewById(R.id.waveform)
        txtState = findViewById(R.id.txtState)
        txtTranscript = findViewById(R.id.txtTranscript)
        txtResponse = findViewById(R.id.txtResponse)

        arcReactor.setOnClickListener {
            if (bound) {
                service?.startListeningCycle()
            } else {
                // Service/permissions not ready yet - re-request instead of doing nothing.
                permissionLauncher.launch(requiredPermissions)
            }
        }

        val txtClock = findViewById<TextView>(R.id.txtClock)
        val txtBattery = findViewById<TextView>(R.id.txtBattery)
        val txtNetwork = findViewById<TextView>(R.id.txtNetwork)
        val txtPerf = findViewById<TextView>(R.id.txtPerf)

        findViewById<TextView>(R.id.txtSettingsBtn).setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java))
        }

        findViewById<TextView>(R.id.txtChatBtn).setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.chat.ChatActivity::class.java))
        }

        findViewById<TextView?>(R.id.btnTalkJarvis)?.setOnClickListener {
            service?.startListeningCycle()
        }
        findViewById<TextView?>(R.id.btnQuickBriefing)?.setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.ui.briefing.BriefingActivity::class.java))
        }
        findViewById<TextView?>(R.id.btnQuickCore)?.setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.ui.SystemCoreActivity::class.java))
        }

        // --- Phase 2: quick actions + bottom nav ---
        fun openPkg(pkg: String) {
            val launch = packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null) startActivity(launch)
            else startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg")))
        }
        findViewById<TextView?>(R.id.btnQuickCall)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL))
        }
        findViewById<TextView?>(R.id.btnQuickMsg)?.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("sms:")))
        }
        findViewById<TextView?>(R.id.btnQuickWa)?.setOnClickListener {
            openPkg("com.whatsapp")
        }
        findViewById<TextView?>(R.id.btnQuickCam)?.setOnClickListener {
            try {
                startActivity(Intent(this, com.jarvis.assistant.vision.VisionActivity::class.java))
            } catch (_: Exception) {

        findViewById<TextView?>(R.id.btnQuickFlash)?.setOnClickListener {
            try {
                val cam = getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
                val id = cam.cameraIdList.firstOrNull()
                if (id != null) {
                    // toggle best-effort via settings panel if torch API restricted
                    startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
                }
            } catch (_: Exception) {
                startActivity(Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS))
            }
        }
        findViewById<TextView?>(R.id.btnQuickWifi)?.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
        }
        findViewById<TextView?>(R.id.btnQuickBt)?.setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS))
        }
        findViewById<TextView?>(R.id.btnQuickMore)?.setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java))
        }
                startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            }
        }
        findViewById<TextView?>(R.id.navHome)?.setOnClickListener { /* already home */ }
        findViewById<TextView?>(R.id.navChat)?.setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.chat.ChatActivity::class.java))
        }
        findViewById<TextView?>(R.id.navMic)?.setOnClickListener {
            service?.startListeningCycle()
        }
        findViewById<TextView?>(R.id.navVision)?.setOnClickListener {
            try {
                startActivity(Intent(this, com.jarvis.assistant.vision.VisionActivity::class.java))
            } catch (_: Exception) { }
        }
        findViewById<TextView?>(R.id.navSettings)?.setOnClickListener {
            startActivity(Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java))
        }


        systemStatus = SystemStatusManager(
            context = this,
            onClockUpdate = { time -> txtClock.text = time },
            onBatteryUpdate = { pct -> txtBattery.text = "BATT: $pct%" }
        )
        systemStatus.start()

        networkStatus = NetworkStatusManager(this)
        perfMonitor = PerformanceMonitor(this)
        startPerfLoop(txtNetwork, txtPerf)

        locationHelper = LocationHelper(this)
        // Weather is optional and online-only - offline commands work with or without this key.
        // Get a free key at openweathermap.org -> API keys tab, or set OPENWEATHER_API_KEY
        // as an env var / gradle property the same way GEMINI_API_KEY is handled.
        weatherClient = WeatherClient(apiKey = BuildConfig.OPENWEATHER_API_KEY)

        settings = com.jarvis.assistant.util.SettingsManager(this)
        settings.getFirstLaunchTime() // records it on the very first call, no-op after that
        arcReactor.setAccentColor(settings.getArcReactorColor())
        scheduleFeedbackPromptCheck()

        permissionLauncher.launch(requiredPermissions)

        handleUnlockIntentIfPresent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUnlockIntentIfPresent(intent)
    }

    /** Voice-triggered "unlock my phone" lands here -- CommandExecutor can't show a
     *  biometric prompt itself (needs a live Activity), so it relaunches MainActivity
     *  with this flag, and this is where the actual prompt gets shown. */
    private fun handleUnlockIntentIfPresent(intent: Intent) {
        if (!intent.getBooleanExtra(UnlockHelper.EXTRA_TRIGGER_UNLOCK, false)) return
        intent.removeExtra(UnlockHelper.EXTRA_TRIGGER_UNLOCK)
        UnlockHelper(this).requestUnlock { statusMessage ->
            txtResponse.text = statusMessage
            service?.let {
                // Speak the result the same way any other command response would be spoken.
                Typewriter.animate(txtResponse, statusMessage)
            }
        }
    }

    /**
     * If the app crashed last time, show the saved stack trace in a copyable dialog.
     * Long-press the text to select and copy it, or take a screenshot — either way, this
     * is enough to diagnose a crash without adb, root, or a PC.
     */
    private fun showLastCrashIfAny() {
        val crashText = CrashHandler.consumeLastCrash(this) ?: return

        val textView = TextView(this).apply {
            text = crashText
            setTextIsSelectable(true)
            setPadding(32, 32, 32, 32)
            textSize = 12f
        }
        val scroll = ScrollView(this).apply { addView(textView) }

        AlertDialog.Builder(this)
            .setTitle("Jarvis crashed last time")
            .setView(scroll)
            .setPositiveButton("OK", null)
            .setCancelable(true)
            .show()
    }

    /**
     * Fires the feedback prompt a fixed delay after the app's very first launch — not per
     * session — so it works whether the person keeps the app open that whole time or closes
     * and reopens it later. Only ever shown once (see SettingsManager.markFeedbackPromptShown).
     */
    private fun scheduleFeedbackPromptCheck() {
        if (settings.hasGivenFeedback()) return

        val delayMs = 2 * 60 * 1000L + 30 * 1000L // 2.5 minutes after first-ever launch
        val elapsed = System.currentTimeMillis() - settings.getFirstLaunchTime()
        val remaining = delayMs - elapsed

        if (remaining <= 0) {
            showFeedbackPrompt()
        } else if (!feedbackCheckPosted) {
            feedbackCheckPosted = true
            feedbackHandler.postDelayed({
                feedbackCheckPosted = false
                if (!isFinishing) showFeedbackPrompt()
            }, remaining)
        }
    }

    private fun showFeedbackPrompt() {
        if (isFinishing || settings.hasGivenFeedback()) return
        AlertDialog.Builder(this)
            .setTitle("Enjoying Jarvis so far?")
            .setMessage("We'd love a quick bit of feedback — takes less than a minute and goes straight to the developer.")
            .setPositiveButton("Give Feedback") { _, _ ->
                startActivity(Intent(this, com.jarvis.assistant.settings.FeedbackActivity::class.java))
            }
            .setNegativeButton("Maybe Later", null)
            .setCancelable(true)
            .show()
    }

    private fun startPerfLoop(txtNetwork: TextView, txtPerf: TextView) {
        val runnable = object : Runnable {
            override fun run() {
                txtNetwork.text = networkStatus.getSignalLabel()
                val ram = perfMonitor.getRamUsagePercent()
                val cpu = perfMonitor.getCpuUsagePercent()
                txtPerf.text = if (cpu >= 0) "CPU:$cpu% RAM:$ram%" else "RAM:$ram%"
                perfHandler.postDelayed(this, 3000)
            }
        }
        perfHandler.post(runnable)
    }

    private fun fetchLocationAndWeather() {
        val txtLocation = findViewById<TextView>(R.id.txtLocation)
        val txtWeather = findViewById<TextView>(R.id.txtWeather)

        activityScope.launch {
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                txtLocation.text = loc.cityName.uppercase()
                val weather = weatherClient.getWeather(loc.lat, loc.lon)
                if (weather != null) {
                    txtWeather.text = "${weather.tempCelsius}\u00b0C  ${weather.condition.uppercase()}"
                } else {
                    txtWeather.text = "WEATHER UNAVAILABLE OFFLINE"
                }
            } else {
                txtLocation.text = "LOCATION UNAVAILABLE"
            }
        }
    }

    private fun startAssistantService() {
        val intent = Intent(this, AssistantForegroundService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    // ---------- AssistantForegroundService.AssistantListener ----------
    // These callbacks arrive on the main thread (the service's CoroutineScope uses
    // Dispatchers.Main), so it's safe to touch views directly here.

    override fun onStateChanged(state: BrainState) {
        arcReactor.state = when (state) {
            BrainState.IDLE -> HudState.IDLE
            BrainState.LISTENING -> HudState.LISTENING
            BrainState.THINKING -> HudState.THINKING
            BrainState.EXECUTING -> HudState.THINKING
            BrainState.SPEAKING -> HudState.SPEAKING
            BrainState.ERROR -> HudState.IDLE
        }
        waveform.active = state == BrainState.LISTENING
        txtState.text = when (state) {
            BrainState.IDLE -> "SYSTEM IDLE"
            BrainState.LISTENING -> "LISTENING..."
            BrainState.THINKING -> "PROCESSING..."
            BrainState.EXECUTING -> "EXECUTING..."
            BrainState.SPEAKING -> "RESPONDING..."
            BrainState.ERROR -> "DIDN'T CATCH THAT"
        }
    }

    override fun onTranscript(text: String) {
        Typewriter.animate(txtTranscript, text)
    }

    override fun onResponse(text: String, fromCloud: Boolean) {
        val prefix = if (fromCloud) "" else "[OFFLINE] "
        Typewriter.animate(txtResponse, prefix + text)
    }

    override fun onResume() {
        com.jarvis.assistant.ui.HudController.addListener(this)
        super.onResume()
        // Catches the case where the process was killed and relaunched after the Handler
        // callback below would have fired — re-checks elapsed time rather than relying only
        // on the originally scheduled callback surviving.
        if (::settings.isInitialized) scheduleFeedbackPromptCheck()

        // Re-apply the arc reactor color every time this screen becomes visible again —
        // setAccentColor() in onCreate() only ran once at launch, so a color changed in
        // SettingsActivity never showed up here until a full app restart. onResume() fires
        // every time the user backs out of Settings, so this is the actual fix.
        if (::arcReactor.isInitialized && ::settings.isInitialized) {
            arcReactor.setAccentColor(settings.getArcReactorColor())
        }
    }

    override fun onDestroy() {
        systemStatus.stop()
        perfHandler.removeCallbacksAndMessages(null)
        feedbackHandler.removeCallbacksAndMessages(null)
        if (bound) {
            service?.listener = null
            unbindService(connection)
        }
        super.onDestroy()
    }

    override fun onHudState(state: com.jarvis.assistant.ui.HudState) {
        runOnUiThread {
            try {
                findViewById<android.widget.TextView?>(com.jarvis.assistant.R.id.txtState)?.text = state.label
                findViewById<android.widget.TextView?>(com.jarvis.assistant.R.id.txtTapHint)?.text = state.label
            } catch (_: Exception) {}
            try {
                val reactor = findViewById<com.jarvis.assistant.ui.ArcReactorView?>(com.jarvis.assistant.R.id.arcReactor)
                reactor?.setListening(state == com.jarvis.assistant.ui.HudState.LISTENING)
                // optional intensity if method exists
                try {
                    val m = reactor?.javaClass?.methods?.find { it.name == "setIntensity" && it.parameterTypes.size == 1 }
                    m?.invoke(reactor, state.intensity)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

}
