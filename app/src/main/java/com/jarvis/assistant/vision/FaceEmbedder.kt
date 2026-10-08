package com.jarvis.assistant.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.hypot

/**
 * Face embedding extractor.
 *
 * Primary path (accurate): place mobile_facenet.tflite in assets/ — optional future.
 * Current path: ML Kit face landmarks + box geometry → fixed-length float vector.
 * Good enough for small personal gallery (family); not for strangers / large crowds.
 */
class FaceEmbedder(private val context: Context) {

    private val detector by lazy {
        val opts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
        FaceDetection.getClient(opts)
    }

    data class FaceHit(
        val box: Rect,
        val embedding: FloatArray,
        val smiling: Float?,
        val leftEyeOpen: Float?,
        val rightEyeOpen: Float?
    )

    suspend fun analyze(bitmap: Bitmap): List<FaceHit> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = detect(image)
        return faces.mapNotNull { face ->
            val emb = embeddingFromFace(face, bitmap.width, bitmap.height) ?: return@mapNotNull null
            FaceHit(
                box = face.boundingBox,
                embedding = emb,
                smiling = face.smilingProbability,
                leftEyeOpen = face.leftEyeOpenProbability,
                rightEyeOpen = face.rightEyeOpenProbability
            )
        }
    }

    private suspend fun detect(image: InputImage): List<Face> =
        suspendCancellableCoroutine { cont ->
            detector.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resume(emptyList()) }
        }

    /**
     * 64-D style vector from landmarks + normalized box + head angles.
     */
    private fun embeddingFromFace(face: Face, imgW: Int, imgH: Int): FloatArray? {
        val box = face.boundingBox
        if (box.width() < 24 || box.height() < 24) return null
        val cx = box.exactCenterX() / imgW
        val cy = box.exactCenterY() / imgH
        val bw = box.width().toFloat() / imgW
        val bh = box.height().toFloat() / imgH

        fun lm(type: Int): Pair<Float, Float>? {
            val p = face.getLandmark(type)?.position ?: return null
            return (p.x - box.left) / box.width().coerceAtLeast(1) to
                (p.y - box.top) / box.height().coerceAtLeast(1)
        }

        val points = listOf(
            FaceLandmark.LEFT_EYE,
            FaceLandmark.RIGHT_EYE,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT,
            FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.MOUTH_BOTTOM,
            FaceLandmark.LEFT_EAR,
            FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_CHEEK,
            FaceLandmark.RIGHT_CHEEK
        ).map { lm(it) }

        val feats = ArrayList<Float>(64)
        feats += cx; feats += cy; feats += bw; feats += bh
        feats += face.headEulerAngleX / 90f
        feats += face.headEulerAngleY / 90f
        feats += face.headEulerAngleZ / 90f
        feats += face.smilingProbability ?: 0.5f
        feats += face.leftEyeOpenProbability ?: 0.5f
        feats += face.rightEyeOpenProbability ?: 0.5f

        for (p in points) {
            if (p != null) {
                feats += p.first
                feats += p.second
            } else {
                feats += 0.5f
                feats += 0.5f
            }
        }

        // pairwise eye distance etc.
        val le = lm(FaceLandmark.LEFT_EYE)
        val re = lm(FaceLandmark.RIGHT_EYE)
        val nose = lm(FaceLandmark.NOSE_BASE)
        if (le != null && re != null) {
            feats += hypot(le.first - re.first, le.second - re.second)
        } else feats += 0.3f
        if (le != null && nose != null) {
            feats += hypot(le.first - nose.first, le.second - nose.second)
        } else feats += 0.2f
        if (re != null && nose != null) {
            feats += hypot(re.first - nose.first, re.second - nose.second)
        } else feats += 0.2f

        while (feats.size < 64) feats += 0f
        return FaceGallery.l2Normalize(feats.take(64).toFloatArray())
    }
}
