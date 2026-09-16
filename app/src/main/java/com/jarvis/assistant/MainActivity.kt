package com.jarvis.assistant

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.jarvis.assistant.brain.BrainState
import com.jarvis.assistant.security.UnlockHelper
import com.jarvis.assistant.service.AssistantForegroundService
import com.jarvis.assistant.ui.HudState
import com.jarvis.assistant.ui.compose.JarvisHudActions
import com.jarvis.assistant.ui.compose.JarvisHudScreen
import com.jarvis.assistant.ui.compose.JarvisHudUiState
import com.jarvis.assistant.util.LocationHelper
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.util.PerformanceMonitor
import com.jarvis.assistant.util.SystemStatusManager
import com.jarvis.assistant.util.WeatherClient


class MainActivity : AppCompatActivity(),
    AssistantForegroundService.AssistantListener,
    com.jarvis.assistant.ui.HudController.Listener {

    private var service: AssistantForegroundService? = null
    private var bound = false

    private lateinit var systemStatus: SystemStatusManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var weatherClient: WeatherClient
    private lateinit var networkStatus: NetworkStatusManager
    private lateinit var perfMonitor: PerformanceMonitor
    private lateinit var settings: com.jarvis.assistant.util.SettingsManager

    private val perfHandler = Handler(Looper.getMainLooper())
    private val feedbackHandler = Handler(Looper.getMainLooper())
    private var feedbackCheckPosted = false

    private val activityScope = CoroutineScope(Dispatchers.Main)

    private var uiState by mutableStateOf(JarvisHudUiState())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        startAssistantService()
        fetchLocationAndWeather()
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AssistantForegroundService.LocalBinder).getService()
            service?.listener = this@MainActivity
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLastCrashIfAny()

        settings = com.jarvis.assistant.util.SettingsManager(this)
        settings.getFirstLaunchTime()

        setContent {
            JarvisHudScreen(
                ui = uiState,
                actions = JarvisHudActions(
                    onTalk = { service?.startListeningCycle() },
                    onReactorTap = {
                        if (bound) service?.startListeningCycle()
                        else permissionLauncher.launch(arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    onChat = {
                        startActivity(Intent(this, com.jarvis.assistant.chat.ChatActivity::class.java))
                    },
                    onSettings = {
                        startActivity(Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java))
                    },
                    onBriefing = {
                        startActivity(Intent(this, com.jarvis.assistant.ui.briefing.BriefingActivity::class.java))
                    },
                    onSystem = {
                        startActivity(Intent(this, com.jarvis.assistant.ui.SystemCoreActivity::class.java))
                    },
                    onVision = {
                        try {
                            startActivity(Intent(this, com.jarvis.assistant.vision.VisionActivity::class.java))
                        } catch (_: Exception) {
                            startActivity(Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
                        }
                    },
                    onArmor = {
                        try {
                            startActivity(Intent(this, com.jarvis.assistant.armor.ArmorHoloArchiveActivity::class.java))
                        } catch (_: Exception) {
                            try {
                                startActivity(Intent(this, com.jarvis.assistant.armor.ArmorSuitsActivity::class.java))
                            } catch (_: Exception) {
                                startActivity(Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java))
                            }
                        }
                    },
                    onNavHome = { },
                    onNavChat = {
                        startActivity(Intent(this, com.jarvis.assistant.chat.ChatActivity::class.java))
                    },
                    onNavMic = { service?.startListeningCycle() },
                    onNavVision = {
                        try {
                            startActivity(Intent(this, com.jarvis.assistant.vision.VisionActivity::class.java))
                        } catch (_: Exception) {}
                    },
                    onNavMore = {
                        startActivity(Intent(this, com.jarvis.assistant.settings.SettingsActivity::class.java))
                    }
                )
            )
        }

        systemStatus = SystemStatusManager(
            context = this,
            onClockUpdate = { time -> uiState = uiState.copy(clock = time) },
            onBatteryUpdate = { pct -> uiState = uiState.copy(battery = "BATT: $pct%") }
        )
        systemStatus.start()

        networkStatus = NetworkStatusManager(this)
        perfMonitor = PerformanceMonitor(this)
        startPerfLoop()

        locationHelper = LocationHelper(this)
        weatherClient = WeatherClient(apiKey = BuildConfig.OPENWEATHER_API_KEY)

        permissionLauncher.launch(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))

        scheduleFeedbackPromptCheck()
        handleUnlockIntentIfPresent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUnlockIntentIfPresent(intent)
    }

    private fun handleUnlockIntentIfPresent(intent: Intent) {
        if (!intent.getBooleanExtra(UnlockHelper.EXTRA_TRIGGER_UNLOCK, false)) return
        intent.removeExtra(UnlockHelper.EXTRA_TRIGGER_UNLOCK)
        UnlockHelper(this).requestUnlock { statusMessage ->
            uiState = uiState.copy(response = statusMessage)
        }
    }

    private fun startPerfLoop() {
        val runnable = object : Runnable {
            override fun run() {
                uiState = uiState.copy(
                    network = networkStatus.getSignalLabel(),
                    ram = "RAM: ${perfMonitor.getRamUsagePercent()}%"
                )
                perfHandler.postDelayed(this, 3000)
            }
        }
        perfHandler.post(runnable)
    }

    private fun fetchLocationAndWeather() {
        activityScope.launch {
            val loc = locationHelper.getCurrentLocation()
            if (loc != null) {
                uiState = uiState.copy(location = loc.cityName.uppercase())
                val weather = weatherClient.getWeather(loc.lat, loc.lon)
                if (weather != null) {
                    uiState = uiState.copy(
                        weather = "${weather.tempCelsius}°C ${weather.condition.uppercase()}"
                    )
                } else {
                    uiState = uiState.copy(weather = "—")
                }
            } else {
                uiState = uiState.copy(location = "—")
            }
        }
    }

    private fun startAssistantService() {
        val intent = Intent(this, AssistantForegroundService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onStateChanged(state: BrainState) {
        val hud = when (state) {
            BrainState.IDLE -> HudState.IDLE
            BrainState.LISTENING -> HudState.LISTENING
            BrainState.THINKING -> HudState.THINKING
            BrainState.EXECUTING -> HudState.THINKING
            BrainState.SPEAKING -> HudState.SPEAKING
            BrainState.ERROR -> HudState.IDLE
        }
        val label = when (state) {
            BrainState.IDLE -> "STANDING BY"
            BrainState.LISTENING -> "LISTENING..."
            BrainState.THINKING -> "PROCESSING..."
            BrainState.EXECUTING -> "EXECUTING..."
            BrainState.SPEAKING -> "RESPONDING..."
            BrainState.ERROR -> "DIDN'T CATCH THAT"
        }
        uiState = uiState.copy(
            hudState = hud,
            stateLabel = label,
            waveformActive = state == BrainState.LISTENING
        )
    }

    override fun onTranscript(text: String) { }

    override fun onResponse(text: String, fromCloud: Boolean) {
        val prefix = if (fromCloud) "" else "[OFFLINE] "
        uiState = uiState.copy(response = prefix + text)
    }

    override fun onHudState(state: HudState) {
        runOnUiThread {
            uiState = uiState.copy(
                hudState = state,
                stateLabel = com.jarvis.assistant.ui.HudController.labelOf(state)
            )
        }
    }

    override fun onResume() {
        com.jarvis.assistant.ui.HudController.addListener(this)
        super.onResume()
        if (::settings.isInitialized) scheduleFeedbackPromptCheck()
    }

    override fun onDestroy() {
        if (::systemStatus.isInitialized) systemStatus.stop()
        perfHandler.removeCallbacksAndMessages(null)
        feedbackHandler.removeCallbacksAndMessages(null)
        if (bound) {
            service?.listener = null
            unbindService(connection)
        }
        super.onDestroy()
    }

    private fun showLastCrashIfAny() {
        try {
            val prefs = getSharedPreferences("jarvis_crash", MODE_PRIVATE)
            val last = prefs.getString("last_crash", null) ?: return
            prefs.edit().remove("last_crash").apply()
            Toast.makeText(this, "Last crash:\n${last.take(200)}", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {}
    }

    private fun scheduleFeedbackPromptCheck() {
        if (!::settings.isInitialized) return
        val delayMs = 3L * 24 * 60 * 60 * 1000
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
}
