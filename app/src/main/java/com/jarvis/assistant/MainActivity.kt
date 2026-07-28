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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), AssistantForegroundService.AssistantListener {

    private var service: AssistantForegroundService? = null
    private var bound = false

    private lateinit var systemStatus: SystemStatusManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var weatherClient: WeatherClient
    private lateinit var networkStatus: NetworkStatusManager
    private lateinit var perfMonitor: PerformanceMonitor
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private val perfHandler = Handler(Looper.getMainLooper())

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
                permissionLauncher.launch(requiredPermissions)
            }
        }

        val txtClock = findViewById<TextView>(R.id.txtClock)
        val txtBattery = findViewById<TextView>(R.id.txtBattery)
        val txtNetwork = findViewById<TextView>(R.id.txtNetwork)
        val txtPerf = findViewById<TextView>(R.id.txtPerf)

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
        weatherClient = WeatherClient(apiKey = BuildConfig.OPENWEATHER_API_KEY)

        permissionLauncher.launch(requiredPermissions)
    }

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

    override fun onDestroy() {
        systemStatus.stop()
        perfHandler.removeCallbacksAndMessages(null)
        if (bound) {
            service?.listener = null
            unbindService(connection)
        }
        super.onDestroy()
    }
}
