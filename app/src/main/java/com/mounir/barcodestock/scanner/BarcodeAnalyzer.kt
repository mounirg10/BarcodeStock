package com.mounir.barcodestock.scanner

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * محلّل الإطارات لقراءة الباركود.
 *
 * @param continuous إذا كان true يستمر المسح (وضع البيع: منتج تلو الآخر)،
 *                   مع تجاهل نفس الباركود خلال [DEBOUNCE_MS] لتجنب التكرار.
 */
class BarcodeAnalyzer(
    private val continuous: Boolean = false,
    private val onBarcode: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private var handled = false
    private var lastValue: String? = null
    private var lastTime = 0L

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128, Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_ITF, Barcode.FORMAT_QR_CODE
            ).build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || (!continuous && handled)) { imageProxy.close(); return }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val value = barcodes.firstOrNull()?.rawValue ?: return@addOnSuccessListener
                val now = System.currentTimeMillis()
                if (continuous) {
                    if (value == lastValue && now - lastTime < DEBOUNCE_MS) return@addOnSuccessListener
                    lastValue = value; lastTime = now
                    onBarcode(value)
                } else if (!handled) {
                    handled = true
                    onBarcode(value)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    companion object { const val DEBOUNCE_MS = 1800L }
}
