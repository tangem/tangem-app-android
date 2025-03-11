package com.tangem.feature.qrscanning.inner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import javax.annotation.concurrent.Immutable

@Immutable
class MLKitBarcodeAnalyzer(private val onScanned: (String) -> Unit) : ImageAnalysis.Analyzer {

    private var isScanning: Boolean = false
    private val scanner = BarcodeScanning.getClient()

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            analyze(image, imageProxy::close)
        }
    }

    fun analyze(image: InputImage, onClose: (() -> Unit)? = null) {
        if (!isScanning) {
            isScanning = true
            scanner.process(image)
                .addOnSuccessListener { codes ->
                    codes.firstOrNull().let { barcode ->
                        val rawValue = barcode?.rawValue
                        rawValue?.let {
                            onScanned.invoke(it)
                        }
                    }
                    isScanning = false
                    onClose?.invoke()
                }
                .addOnFailureListener {
                    isScanning = false
                    onClose?.invoke()
                }
        }
    }
}