package com.tangem.feature.qrscanning

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.mlkit.vision.common.InputImage
import com.tangem.core.ui.UiDependencies
import com.tangem.core.ui.components.SystemBarsIconsDisposable
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.feature.qrscanning.inner.MLKitBarcodeAnalyzer
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import com.tangem.feature.qrscanning.presentation.QrScanningContent
import com.tangem.feature.qrscanning.viewmodel.QrScanningViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
internal class QrScanningFragment : ComposeFragment() {

    @Inject
    override lateinit var uiDependencies: UiDependencies

    @Inject
    lateinit var router: QrScanningRouter

    private val innerRouter: QrScanningInnerRouter
        get() = requireNotNull(router as? QrScanningInnerRouter) {
            "innerRouter should be instance of QrScanningInnerRouter"
        }

    private val viewModel by viewModels<QrScanningViewModel>()

    private var cameraExecutor: ExecutorService by Delegates.notNull()
    // Camera requires its own analyzer instance due to flow of frames needed to be analyzed.
    // Each new frame can cancel previous analysis e.i. image from the gallery can be skipped.
    private val cameraAnalyzer: MLKitBarcodeAnalyzer by lazy(LazyThreadSafetyMode.NONE) {
        MLKitBarcodeAnalyzer(viewModel::onQrScanned)
    }
    private val analyzer: MLKitBarcodeAnalyzer by lazy(LazyThreadSafetyMode.NONE) {
        MLKitBarcodeAnalyzer(viewModel::onQrScanned)
    }

    private val cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!it) viewModel.onCameraDeniedState()
    }
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
        val selectedImage = it ?: Uri.EMPTY
        if (selectedImage != Uri.EMPTY) {
            val image = InputImage.fromFilePath(requireContext(), selectedImage)
            analyzer.analyze(image)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setRouter(innerRouter)
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermission()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.launchGalleryEvent
                    .collect {
                        galleryLauncher.launch(it.imageFilter)
                        delay(timeMillis = 2000)
                    }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissionGranted()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraPermissionLauncher.unregister()
        cameraExecutor.shutdown()
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        SystemBarsIconsDisposable(darkIcons = false)

        QrScanningContent(
            executor = { cameraExecutor },
            analyzer = { cameraAnalyzer },
            uiState = viewModel.uiState.collectAsStateWithLifecycle().value,
        )
    }

    /**
     * Method for requesting permission if there isn't one.
     */
    private fun requestCameraPermission() {
        if (
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_DENIED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    /**
     * Method for checking if permission was granted after user opened Settings screen.
     * If permission was granted dismiss bottom sheet.
     */
    private fun checkPermissionGranted() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onDismissBottomSheetState()
        }
    }

    companion object {

        fun create() = QrScanningFragment()
    }
}
