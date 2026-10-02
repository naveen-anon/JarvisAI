package com.jarvis.assistant.vision

import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object BarcodeHelper {
    suspend fun scan(image: InputImage): String = suspendCancellableCoroutine { cont ->
        val scanner = BarcodeScanning.getClient()
        scanner.process(image)
            .addOnSuccessListener { codes ->
                if (codes.isEmpty()) {
                    cont.resume("No barcode or QR code found.")
                    return@addOnSuccessListener
                }
                val parts = codes.map { b ->
                    val kind = when (b.valueType) {
                        Barcode.TYPE_URL -> "URL"
                        Barcode.TYPE_EMAIL -> "email"
                        Barcode.TYPE_PHONE -> "phone"
                        Barcode.TYPE_WIFI -> "Wi‑Fi"
                        Barcode.TYPE_CONTACT_INFO -> "contact"
                        else -> "code"
                    }
                    "$kind: ${b.rawValue ?: b.displayValue ?: "—"}"
                }
                cont.resume("Found ${parts.size}: " + parts.joinToString("; ") + ".")
            }
            .addOnFailureListener { e ->
                cont.resume("Barcode scan failed: ${e.message}")
            }
    }
}
