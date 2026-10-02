package com.jarvis.assistant.vision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.jarvis.assistant.R
import com.jarvis.assistant.voice.TextToSpeechHelper
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Foreground security watch: continuous on-device face presence.
 * Alert debounce \~10s. For the device owner's own space only.
 */
class PresenceWatchActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var statusText: TextView
    private lateinit var tts: TextToSpeechHelper
    private val analyzerExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val analyzing = AtomicBoolean(false)
    private val lastAlertMs = AtomicLong(0L)
    private var detections = 0

    private val requestCam = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else {
            statusText.text = "Camera permission required."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presence_watch)
        previewView = findViewById(R.id.previewWatch)
        statusText = findViewById(R.id.txtWatchStatus)
        tts = TextToSpeechHelper(this)

        findViewById<android.view.View>(R.id.btnWatchStop).setOnClickListener {
            tts.speak("Security watch stopped.")
            finish()
        }

        statusText.text = "ARMED · watching for faces"
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) startCamera()
        else requestCam.launch(Manifest.permission.CAMERA)

        tts.speak("Security watch armed.")
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(analyzerExecutor) { proxy ->
                if (!analyzing.compareAndSet(false, true)) {
                    proxy.close()
                    return@setAnalyzer
                }
                try {
                    val media = proxy.image
                    if (media != null) {
                        val image = InputImage.fromMediaImage(
                            media, proxy.imageInfo.rotationDegrees
                        )
                        // Blocking-style via runBlocking not ideal; use Tasks await pattern lightly
                        val task = com.google.mlkit.vision.face.FaceDetection
                            .getClient(
                                com.google.mlkit.vision.face.FaceDetectorOptions.Builder()
                                    .setPerformanceMode(
                                        com.google.mlkit.vision.face.FaceDetectorOptions.PERFORMANCE_MODE_FAST
                                    )
                                    .build()
                            )
                            .process(image)
                        task.addOnSuccessListener { faces ->
                            val n = faces.size
                            mainHandler.post {
                                if (n > 0) {
                                    detections++
                                    statusText.text = "ALERT · $n face(s) · events $detections"
                                    val now = System.currentTimeMillis()
                                    if (now - lastAlertMs.get() > 10_000L) {
                                        lastAlertMs.set(now)
                                        try {
                                            tts.speak(
                                                if (n == 1) "Presence detected."
                                                else "Multiple faces detected."
                                            )
                                        } catch (_: Exception) {}
                                    }
                                } else {
                                    statusText.text = "ARMED · clear · events $detections"
                                }
                            }
                        }.addOnCompleteListener {
                            analyzing.set(false)
                            proxy.close()
                        }
                        return@setAnalyzer
                    }
                } catch (_: Exception) {
                }
                analyzing.set(false)
                proxy.close()
            }
            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis
                )
            } catch (e: Exception) {
                // fallback back camera
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                } catch (e2: Exception) {
                    statusText.text = "Camera error: ${e2.message}"
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        try { analyzerExecutor.shutdown() } catch (_: Exception) {}
        try { tts.shutdown() } catch (_: Exception) {}
    }
}
