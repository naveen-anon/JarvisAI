package com.jarvis.assistant.ui.clock

import android.content.Intent
import android.media.RingtoneManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.AlarmClock
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R

/**
 * Personal Jarvis clock: Alarm (system), Timer (in-app + system), Stopwatch (in-app).
 * Open via voice: "open clock", "start stopwatch", "set timer for 5 minutes".
 */
class ClockActivity : AppCompatActivity() {

    private lateinit var tabAlarm: TextView
    private lateinit var tabTimer: TextView
    private lateinit var tabStopwatch: TextView
    private lateinit var panelAlarm: LinearLayout
    private lateinit var panelTimer: LinearLayout
    private lateinit var panelStopwatch: LinearLayout

    private lateinit var txtTimerDisplay: TextView
    private lateinit var txtStopwatchDisplay: TextView
    private lateinit var inputTimerMin: EditText
    private lateinit var inputTimerSec: EditText
    private lateinit var inputAlarmHour: EditText
    private lateinit var inputAlarmMin: EditText

    private var countDown: CountDownTimer? = null
    private var timerRunning = false
    private var timerLeftMs = 0L

    private var swRunning = false
    private var swBase = 0L
    private var swPauseOffset = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val swTick = object : Runnable {
        override fun run() {
            if (swRunning) {
                txtStopwatchDisplay.text = formatMs(elapsedSw())
                handler.postDelayed(this, 50)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_clock)

        tabAlarm = findViewById(R.id.tabAlarm)
        tabTimer = findViewById(R.id.tabTimer)
        tabStopwatch = findViewById(R.id.tabStopwatch)
        panelAlarm = findViewById(R.id.panelAlarm)
        panelTimer = findViewById(R.id.panelTimer)
        panelStopwatch = findViewById(R.id.panelStopwatch)
        txtTimerDisplay = findViewById(R.id.txtTimerDisplay)
        txtStopwatchDisplay = findViewById(R.id.txtStopwatchDisplay)
        inputTimerMin = findViewById(R.id.inputTimerMin)
        inputTimerSec = findViewById(R.id.inputTimerSec)
        inputAlarmHour = findViewById(R.id.inputAlarmHour)
        inputAlarmMin = findViewById(R.id.inputAlarmMin)

        findViewById<TextView>(R.id.btnClockBack).setOnClickListener { finish() }

        tabAlarm.setOnClickListener { showTab(0) }
        tabTimer.setOnClickListener { showTab(1) }
        tabStopwatch.setOnClickListener { showTab(2) }

        findViewById<TextView>(R.id.btnSetAlarm).setOnClickListener { setSystemAlarm() }

        // AM/PM + steppers (HUD)
        var isPm = false
        fun paintAmPm() {
            val am = findViewById<TextView>(R.id.btnAm)
            val pm = findViewById<TextView>(R.id.btnPm)
            if (isPm) {
                am.setBackgroundResource(R.drawable.glass_button_bg)
                am.setTextColor(0xFF00D9FF.toInt())
                pm.setBackgroundResource(R.drawable.glass_button_filled)
                pm.setTextColor(0xFF03080E.toInt())
            } else {
                am.setBackgroundResource(R.drawable.glass_button_filled)
                am.setTextColor(0xFF03080E.toInt())
                pm.setBackgroundResource(R.drawable.glass_button_bg)
                pm.setTextColor(0xFF00D9FF.toInt())
            }
        }
        findViewById<TextView>(R.id.btnAm).setOnClickListener { isPm = false; paintAmPm() }
        findViewById<TextView>(R.id.btnPm).setOnClickListener { isPm = true; paintAmPm() }
        fun bump(et: EditText, delta: Int, minV: Int, maxV: Int) {
            val v = (et.text.toString().toIntOrNull() ?: minV) + delta
            val n = when {
                v > maxV -> minV
                v < minV -> maxV
                else -> v
            }
            et.setText(n.toString().padStart(2, '0'))
        }
        findViewById<TextView>(R.id.btnHourUp).setOnClickListener { bump(inputAlarmHour, 1, 1, 12) }
        findViewById<TextView>(R.id.btnHourDown).setOnClickListener { bump(inputAlarmHour, -1, 1, 12) }
        findViewById<TextView>(R.id.btnMinUp).setOnClickListener { bump(inputAlarmMin, 1, 0, 59) }
        findViewById<TextView>(R.id.btnMinDown).setOnClickListener { bump(inputAlarmMin, -1, 0, 59) }
        // tag isPm for setSystemAlarm
        findViewById<TextView>(R.id.btnAm).tag = false
        findViewById<TextView>(R.id.btnPm).setOnClickListener {
            isPm = true
            findViewById<TextView>(R.id.btnAm).tag = true
            paintAmPm()
        }
        findViewById<TextView>(R.id.btnAm).setOnClickListener {
            isPm = false
            findViewById<TextView>(R.id.btnAm).tag = false
            paintAmPm()
        }

        findViewById<TextView>(R.id.btnTimerStart).setOnClickListener { startTimer() }
        findViewById<TextView>(R.id.btnTimerPause).setOnClickListener { pauseTimer() }
        findViewById<TextView>(R.id.btnTimerReset).setOnClickListener { resetTimer() }
        findViewById<TextView>(R.id.btnSwStart).setOnClickListener { startStopwatch() }
        findViewById<TextView>(R.id.btnSwStart).setOnClickListener { pauseStopwatch() }
        findViewById<TextView>(R.id.btnSwReset).setOnClickListener { resetStopwatch() }

        when (intent.getStringExtra(EXTRA_MODE)?.lowercase()) {
            "timer" -> {
                showTab(1)
                val sec = intent.getIntExtra(EXTRA_SECONDS, 0)
                if (sec > 0) {
                    timerLeftMs = sec * 1000L
                    txtTimerDisplay.text = formatMs(timerLeftMs)
                    inputTimerMin.setText((sec / 60).toString())
                    inputTimerSec.setText((sec % 60).toString())
                    if (intent.getBooleanExtra(EXTRA_AUTO_START, false)) startTimer()
                }
            }
            "stopwatch" -> {
                showTab(2)
                if (intent.getBooleanExtra(EXTRA_AUTO_START, false)) startStopwatch()
            }
            else -> showTab(0)
        }
    }

    private fun showTab(index: Int) {
        panelAlarm.visibility = if (index == 0) View.VISIBLE else View.GONE
        panelTimer.visibility = if (index == 1) View.VISIBLE else View.GONE
        panelStopwatch.visibility = if (index == 2) View.VISIBLE else View.GONE
        val onBg = R.drawable.glass_button_filled
        val offBg = R.drawable.glass_button_bg
        val onC = 0xFF03080E.toInt()
        val offC = 0xFF00D9FF.toInt()
        tabAlarm.setBackgroundResource(if (index == 0) onBg else offBg)
        tabTimer.setBackgroundResource(if (index == 1) onBg else offBg)
        tabStopwatch.setBackgroundResource(if (index == 2) onBg else offBg)
        tabAlarm.setTextColor(if (index == 0) onC else offC)
        tabTimer.setTextColor(if (index == 1) onC else offC)
        tabStopwatch.setTextColor(if (index == 2) onC else offC)
    }

    private fun setSystemAlarm() {
        var h = inputAlarmHour.text.toString().toIntOrNull() ?: 7
        val min = (inputAlarmMin.text.toString().toIntOrNull() ?: 0).coerceIn(0, 59)
        val pm = (findViewById<TextView>(R.id.btnAm).tag as? Boolean) == true
        // 12h UI → 24h for AlarmClock
        h = h.coerceIn(1, 12)
        var hour = h % 12
        if (pm) hour += 12
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, min)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            // no system clock — still close gracefully
        }
    }

    private fun startTimer() {
        if (timerRunning) return
        if (timerLeftMs <= 0L) {
            val mins = inputTimerMin.text.toString().toIntOrNull() ?: 0
            val secs = inputTimerSec.text.toString().toIntOrNull() ?: 0
            timerLeftMs = ((mins * 60) + secs).coerceAtLeast(1) * 1000L
        }
        timerRunning = true
        countDown?.cancel()
        countDown = object : CountDownTimer(timerLeftMs, 200) {
            override fun onTick(ms: Long) {
                timerLeftMs = ms
                txtTimerDisplay.text = formatMs(ms)
            }
            override fun onFinish() {
                timerRunning = false
                timerLeftMs = 0L
                txtTimerDisplay.text = "00:00"
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    RingtoneManager.getRingtone(this@ClockActivity, uri)?.play()
                } catch (_: Exception) {}
            }
        }.start()
    }

    private fun pauseTimer() {
        countDown?.cancel()
        timerRunning = false
    }

    private fun resetTimer() {
        countDown?.cancel()
        timerRunning = false
        timerLeftMs = 0L
        txtTimerDisplay.text = "00:00"
    }

    private fun elapsedSw(): Long =
        if (swRunning) SystemClock.elapsedRealtime() - swBase + swPauseOffset else swPauseOffset

    private fun startStopwatch() {
        if (swRunning) return
        swBase = SystemClock.elapsedRealtime()
        swRunning = true
        handler.post(swTick)
    }

    private fun pauseStopwatch() {
        if (!swRunning) return
        swPauseOffset = elapsedSw()
        swRunning = false
        handler.removeCallbacks(swTick)
        txtStopwatchDisplay.text = formatMs(swPauseOffset)
    }

    private fun resetStopwatch() {
        swRunning = false
        handler.removeCallbacks(swTick)
        swBase = 0L
        swPauseOffset = 0L
        txtStopwatchDisplay.text = "00:00.00"
    }

    private fun formatMs(ms: Long): String {
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        val cs = (ms % 1000) / 10
        return "%02d:%02d.%02d".format(m, s, cs)
    }

    override fun onDestroy() {
        super.onDestroy()
        countDown?.cancel()
        handler.removeCallbacks(swTick)
    }

    companion object {
        const val EXTRA_MODE = "clock_mode"
        const val EXTRA_SECONDS = "timer_seconds"
        const val EXTRA_AUTO_START = "auto_start"
    }
}
