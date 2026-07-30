package com.jarvis.assistant.vision

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Phase 4 — "Live camera analysis". One screen, three on-device vision modes selected by
 * the [EXTRA_MODE] intent extra ("ocr" | "objects" | "faces"), or picked manually with the
 * on-screen buttons. Captures a single frame via CameraX, hands it to ML Kit, speaks and
 * displays the result. Everything here runs on-device — no image ever leaves the phone.
 */
class VisionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var tts: TextToSpeechHelper
    private var imageCapture: ImageCapture? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startCamera() else resultText.text = "Camera permission is required." }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeechHelper(this)
        setContentView(buildUi())

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // If launched directly from a voice command, auto-run that mode once the camera's ready.
        intent.getStringExtra(EXTRA_MODE)?.let { mode ->
            resultText.text = "Point the camera, capturing in a moment…"
            previewView.postDelayed({ runAnalysis(mode) }, 800)
        }
    }

    private fun buildUi(): LinearLayout {
        previewView = PreviewView(this)
        resultText = TextView(this).apply {
            setTextColor(Color.parseColor("#00D4FF"))
            textSize = 15f
            setPadding(24, 24, 24, 24)
        }

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("ocr" to "Scan Text", "objects" to "Detect Objects", "faces" to "Faces").forEach { (mode, label) ->
            buttonRow.addView(Button(this).apply {
                text = label
                setOnClickListener { runAnalysis(mode) }
            })
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#050A0F"))
        }
        root.addView(previewView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(buttonRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300)
            addView(resultText)
        }
        root.addView(scroll)
        return root
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                provider.unbindAll()
                provider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                resultText.text = "Couldn't start the camera: ${e.message}"
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun runAnalysis(mode: String) {
        val capture = imageCapture ?: run {
            resultText.text = "Camera isn't ready yet."
            return
        }
        resultText.text = "Analyzing…"

        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val mediaImage = image.image
                    if (mediaImage == null) {
                        image.close()
                        resultText.text = "Couldn't read the captured frame."
                        return
                    }
                    val inputImage = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
                    scope.launch {
                        val result = when (mode) {
                            "ocr" -> TextRecognitionHelper.recognize(inputImage)
                            "objects" -> ObjectDetectionHelper.detect(inputImage)
                            "faces" -> FaceDetectionHelper.detect(inputImage)
                            else -> "Unknown vision mode."
                        }
                        image.close()
                        resultText.text = result
                        tts.speak(result)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    resultText.text = "Capture failed: ${exception.message}"
                }
            }
        )
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_MODE = "vision_mode"
    }
}
