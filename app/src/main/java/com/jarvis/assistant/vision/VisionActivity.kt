package com.jarvis.assistant.vision

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import com.jarvis.assistant.R
import com.jarvis.assistant.voice.TextToSpeechHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Live camera analysis — OCR / objects / faces (on-device ML Kit).
 * Launch with EXTRA_MODE = "ocr" | "objects" | "faces" (optional auto-run).
 */
class VisionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var btnOcr: TextView
    private lateinit var btnObjects: TextView
    private lateinit var btnFaces: TextView
    private lateinit var tts: TextToSpeechHelper
    private var imageCapture: ImageCapture? = null
    private var currentMode: String = "ocr"
    private val scope = CoroutineScope(Dispatchers.Main)

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else resultText.text = "Camera permission is required, sir."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vision)
        tts = TextToSpeechHelper(this)

        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.txtVisionResult)
        btnOcr = findViewById(R.id.btnModeOcr)
        btnObjects = findViewById(R.id.btnModeObjects)
        btnFaces = findViewById(R.id.btnModeFaces)

        findViewById<TextView>(R.id.btnVisionClose).setOnClickListener { finish() }

        btnOcr.setOnClickListener { selectMode("ocr"); runAnalysis("ocr") }
        btnObjects.setOnClickListener { selectMode("objects"); runAnalysis("objects") }
        btnFaces.setOnClickListener { selectMode("faces"); runAnalysis("faces") }
        findViewById<TextView>(R.id.btnCapture).setOnClickListener { runAnalysis(currentMode) }

        currentMode = intent.getStringExtra(EXTRA_MODE)?.lowercase() ?: "ocr"
        selectMode(currentMode)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
            // Voice-launched: auto capture after preview settles
            if (intent.hasExtra(EXTRA_MODE)) {
                previewView.postDelayed({ runAnalysis(currentMode) }, 900)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun selectMode(mode: String) {
        currentMode = mode
        val active = 0xFF00E5FF.toInt()
        val idle = 0xFF5C8A94.toInt()
        btnOcr.setTextColor(if (mode == "ocr") active else idle)
        btnObjects.setTextColor(if (mode == "objects") active else idle)
        btnFaces.setTextColor(if (mode == "faces") active else idle)
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
        val capture = imageCapture
        if (capture == null) {
            resultText.text = "Camera not ready yet."
            return
        }
        resultText.text = "Analyzing…"
        capture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    scope.launch {
                        try {
                            val media = image.image
                            if (media == null) {
                                resultText.text = "Empty frame."
                                image.close()
                                return@launch
                            }
                            val inputImage = InputImage.fromMediaImage(
                                media, image.imageInfo.rotationDegrees
                            )
                            val result = when (mode) {
                                "ocr" -> TextRecognitionHelper.recognize(inputImage)
                                "objects" -> ObjectDetectionHelper.detect(inputImage)
                                "faces" -> FaceDetectionHelper.detect(inputImage)
                                else -> "Unknown vision mode."
                            }
                            resultText.text = result
                            tts.speak(result)
                        } catch (e: Exception) {
                            resultText.text = "Analysis failed: ${e.message}"
                        } finally {
                            image.close()
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    resultText.text = "Capture failed: ${exception.message}"
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try { tts.shutdown() } catch (_: Exception) {}
    }

    companion object {
        const val EXTRA_MODE = "vision_mode"
    }
}
