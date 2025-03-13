package com.tangem.feature.qrscanning

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.google.mlkit.vision.common.InputImage
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.feature.qrscanning.inner.MLKitBarcodeAnalyzer
import com.tangem.feature.qrscanning.model.QrScanningModel
import com.tangem.feature.qrscanning.presentation.QrScanningContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.delay
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class DefaultQrScanningComponent @AssistedInject constructor(
    @Assisted appComponentContext: AppComponentContext,
    @Assisted params: QrScanningComponent.Params,
) : QrScanningComponent, AppComponentContext by appComponentContext {

    private val model: QrScanningModel = getOrCreateModel(params)

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    // Camera requires its own analyzer instance due to flow of frames needed to be analyzed.
    // Each new frame can cancel previous analysis e.i. image from the gallery can be skipped.
    private val cameraAnalyzer: MLKitBarcodeAnalyzer by lazy(LazyThreadSafetyMode.NONE) {
        MLKitBarcodeAnalyzer(model::onQrScanned)
    }
    private val analyzer: MLKitBarcodeAnalyzer by lazy(LazyThreadSafetyMode.NONE) {
        MLKitBarcodeAnalyzer(model::onQrScanned)
    }

    init {
        lifecycle.doOnDestroy { cameraExecutor.shutdown() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val context = LocalContext.current

        val cameraPermissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted.not()) {
                    model.onCameraDeniedState()
                }
            }

        val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
            val selectedImage = it ?: Uri.EMPTY
            if (selectedImage != Uri.EMPTY) {
                val mimeType = context.contentResolver.getType(selectedImage)
                if (mimeType.isImageMimeType()) {
                    try {
                        val image = InputImage.fromFilePath(context, selectedImage)
                        analyzer.analyze(image)
                    } catch (e: IOException) {
                        Timber.e(e, "Unable to get image $selectedImage from gallery")
                    }
                }
            }
        }

        LaunchedEffect(Unit) {
            model.launchGallery.collect {
                galleryLauncher.launch(GALLERY_IMAGE_FILTER)
                delay(timeMillis = 2000)
            }
        }

        LifecycleEventEffect(
            event = Lifecycle.Event.ON_CREATE,
        ) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_DENIED
            ) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        LifecycleEventEffect(
            event = Lifecycle.Event.ON_RESUME,
        ) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA,
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                model.onDismissBottomSheetState()
            }
        }

        ScreenContent(modifier)
    }

    @Suppress("UnusedPrivateMember")
    @Composable
    private fun ScreenContent(modifier: Modifier = Modifier) {
        SystemBarsIconsDisposable(darkIcons = false)

        QrScanningContent(
            executor = { cameraExecutor },
            analyzer = { cameraAnalyzer },
            uiState = model.uiState.collectAsStateWithLifecycle().value,
        )
    }

    private fun String?.isImageMimeType() = this?.startsWith(prefix = "$IMAGE_MIME_TYPE/") == true

    @AssistedFactory
    interface Factory : QrScanningComponent.Factory {
        override fun create(
            context: AppComponentContext,
            params: QrScanningComponent.Params,
        ): DefaultQrScanningComponent
    }

    companion object {

        private const val IMAGE_MIME_TYPE = "image"
        private const val GALLERY_IMAGE_FILTER = "$IMAGE_MIME_TYPE/*"
    }
}