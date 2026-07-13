package com.jarvis.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.service.AssistantForegroundService
import com.jarvis.assistant.ui.ArcReactorView
import com.jarvis.assistant.util.LocationHelper
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.util.PerformanceMonitor
import com.jarvis.assistant.util.SystemStatusManager
import com.jarvis.assistant.util.WeatherClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var service: AssistantForegroundService? = null
    private var bound = false

    private lateinit var systemStatus: SystemStatusManager
    private lateinit var locationHelper: LocationHelper
    private lateinit var weatherClient: WeatherClient
    private lateinit var networkStatus: NetworkStatusManager
    private lateinit var perfMonitor: PerformanceMonitor
    private val activityScope = CoroutineScope(Dispatchers.Main)
    private val perfHandler = Handler(Looper.getMainLooper())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AssistantForegroundService.LocalBinder).getService()
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
        android.Manifest.permission.ACCESS_COARSE_LOCATION
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

        val arcReactor = findViewById<ArcReactorView>(R.id.arcReactor)
        arcReactor.setOnClickListener {
            service?.startListeningCycle()
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
        // Paste your free OpenWeatherMap key here (openweathermap.org -> API keys tab)
        weatherClient = WeatherClient(apiKey = "YOUR_OPENWEATHERMAP_KEY")

        permissionLauncher.launch(requiredPermissions)
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
                    txtWeather.text = "${weather.tempCelsius}°C  ${weather.condition.uppercase()}"
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

    override fun onDestroy() {
        systemStatus.stop()
        perfHandler.removeCallbacksAndMessages(null)
        if (bound) unbindService(connection)
        super.onDestroy()
    }
}
