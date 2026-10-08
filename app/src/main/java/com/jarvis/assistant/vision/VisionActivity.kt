package com.jarvis.assistant.vision

import java.io.ByteArrayOutputStream

import android.graphics.YuvImage

import android.graphics.Rect

import android.graphics.ImageFormat

import android.graphics.BitmapFactory

import android.graphics.Bitmap

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
        findViewById<TextView>(R.id.btnModeLabels).setOnClickListener {
            selectMode("labels"); runAnalysis("labels")
        }
        findViewById<TextView>(R.id.btnModeBarcode).setOnClickListener {
            selectMode("barcode"); runAnalysis("barcode")
        }
        findViewById<TextView>(R.id.btnCapture).setOnClickListener { runAnalysis(currentMode) }

        currentMode = intent.getStringExtra(EXTRA_MODE)?.lowercase() ?: "scene"
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
        val active = 0xFF00D9FF.toInt()
        val idle = 0xFF5A8A99.toInt()
        btnOcr.setTextColor(if (mode == "ocr") active else idle)
        btnObjects.setTextColor(if (mode == "objects") active else idle)
        btnFaces.setTextColor(if (mode == "faces") active else idle)
        try {
            findViewById<TextView>(R.id.btnModeLabels)
                .setTextColor(if (mode == "labels" || mode == "scene") active else idle)
            findViewById<TextView>(R.id.btnModeBarcode)
                .setTextColor(if (mode == "barcode") active else idle)
        } catch (_: Exception) {}
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
                                "labels" -> ImageLabelHelper.label(inputImage)
                                "barcode" -> BarcodeHelper.scan(inputImage)
                                "scene", "full" -> {
                                    val ocr = try { TextRecognitionHelper.recognize(inputImage) } catch (_: Exception) { "" }
                                    val objs = try { ObjectDetectionHelper.detect(inputImage) } catch (_: Exception) { "" }
                                    val faces = try { FaceDetectionHelper.detect(inputImage) } catch (_: Exception) { "" }
                                    val labels = try { ImageLabelHelper.label(inputImage) } catch (_: Exception) { "" }
                                    val codes = try { BarcodeHelper.scan(inputImage) } catch (_: Exception) { "" }
                                    buildString {
                                        append("Scene analysis. ")
                                        if (labels.isNotBlank() && !labels.contains("don't recognize", ignoreCase = true))
                                            append(labels.trim()).append(" ")
                                        if (objs.isNotBlank() && !objs.contains("don't recognize", ignoreCase = true))
                                            append(objs.trim()).append(" ")
                                        if (faces.isNotBlank() && !faces.contains("No ", ignoreCase = true) && !faces.contains("0 face", ignoreCase = true))
                                            append(faces.trim()).append(" ")
                                        if (codes.isNotBlank() && !codes.startsWith("No barcode"))
                                            append(codes.trim()).append(" ")
                                        if (ocr.isNotBlank() && ocr.length > 3 && !ocr.contains("No text", ignoreCase = true))
                                            append("Visible text: ").append(ocr.take(280).trim())
                                        if (length <= "Scene analysis. ".length)
                                            append("I don't see clear labels, objects, faces, codes, or text in this frame, sir.")
                                    }.trim()
                                }
                                
                                "identify", "identity", "who" -> {
                                    val bmp = imageProxyToBitmap(image)
                                    if (bmp == null) {
                                        "Couldn't read camera frame, sir."
                                    } else {
                                        val ids = FaceIdentityHelper.identify(this@VisionActivity, bmp)
                                        if (ids.isEmpty()) {
                                            "No face in frame, sir."
                                        } else {
                                            ids.joinToString(" ") {
                                                if (it.known) "Identity ${it.label}, ${it.detail}."
                                                else "Unknown person."
                                            }
                                        }
                                    }
                                }
                                "enroll" -> {
                                    val name = intent.getStringExtra("enroll_name") ?: "User"
                                    val bmp = imageProxyToBitmap(image)
                                    if (bmp == null) {
                                        "Couldn't read camera frame, sir."
                                    } else {
                                        FaceIdentityHelper.enrollFromBitmap(this@VisionActivity, bmp, name)
                                    }
                                }

                                else -> "Unknown vision mode."
                            }
                            val spoken = result.replace(Regex("\\s+"), " ").trim().let {
                                if (it.length > 400) it.take(400) + "…" else it
                            }
                            resultText.text = result
                            tts.speak(spoken)
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

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val bytes = out.toByteArray()
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

}
