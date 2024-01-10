package com.tangem.feature.qrscanning.inner

import android.content.Context
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleOwner
import com.tangem.feature.qrscanning.impl.databinding.LayoutQrScanningBinding
import java.util.concurrent.ExecutorService

internal class PreviewBinder {

    @Suppress("LongParameterList")
    fun bindPreview(
        context: Context,
        binding: LayoutQrScanningBinding,
        lifecycleOwner: LifecycleOwner,
        cameraProvider: ProcessCameraProvider,
        cameraExecutor: ExecutorService,
        onScanned: (String) -> Unit,
    ) {
        cameraProvider.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(binding.cameraPreview.width, binding.cameraPreview.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val orientationEventListener = object : OrientationEventListener(context) {

            @Suppress("MagicNumber")
            override fun onOrientationChanged(orientation: Int) {
                val rotation: Int = when (orientation) {
                    in 45..134 -> Surface.ROTATION_270
                    in 135..224 -> Surface.ROTATION_180
                    in 225..314 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }

                imageAnalysis.targetRotation = rotation
            }
        }
        orientationEventListener.enable()

        val analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer {
            imageAnalysis.clearAnalyzer()
            onScanned.invoke(it)
        }

        cameraExecutor.let {
            imageAnalysis.setAnalyzer(it, analyzer)
        }

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageAnalysis, preview)
    }
}