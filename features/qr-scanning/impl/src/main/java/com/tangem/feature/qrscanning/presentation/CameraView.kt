package com.tangem.feature.qrscanning.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.tangem.feature.qrscanning.inner.MLKitBarcodeAnalyzer
import java.util.concurrent.ExecutorService

@Composable
internal fun CameraView(executor: () -> ExecutorService, analyzer: () -> MLKitBarcodeAnalyzer, isFlash: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val cameraController = remember { LifecycleCameraController(context) }

    cameraController.apply {
        bindToLifecycle(lifecycleOwner)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        setImageAnalysisAnalyzer(executor(), analyzer())
        imageAnalysisBackpressureStrategy = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
    }

    LaunchedEffect(key1 = isFlash) {
        cameraController.cameraControl?.enableTorch(isFlash)
    }

    previewView.controller = cameraController

    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}