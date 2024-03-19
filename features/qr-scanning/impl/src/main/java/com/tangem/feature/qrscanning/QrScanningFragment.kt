package com.tangem.feature.qrscanning

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.mlkit.vision.common.InputImage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.screen.ComposeFragment
import com.tangem.core.ui.theme.AppThemeModeHolder
import com.tangem.feature.qrscanning.inner.MLKitBarcodeAnalyzer
import com.tangem.feature.qrscanning.navigation.QrScanningInnerRouter
import com.tangem.feature.qrscanning.presentation.QrScanningContent
import com.tangem.feature.qrscanning.viewmodel.QrScanningViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
internal class QrScanningFragment : ComposeFragment() {

    @Inject
    override lateinit var appThemeModeHolder: AppThemeModeHolder

    @Inject
    lateinit var router: QrScanningRouter

    private val innerRouter: QrScanningInnerRouter
        get() = requireNotNull(router as? QrScanningInnerRouter) {
            "innerRouter should be instance of QrScanningInnerRouter"
        }

    private val viewModel by viewModels<QrScanningViewModel>()

    private var cameraExecutor: ExecutorService by Delegates.notNull()
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
        viewModel.setRouter(innerRouter, galleryLauncher)
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermission()
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
        StatusBarTransparencyDisposable()
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

@Composable
private fun StatusBarTransparencyDisposable() {
    val systemUiController = rememberSystemUiController()
    val systemBarsColor = TangemTheme.colors.background.secondary
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                systemUiController.setSystemBarsColor(
                    color = Color.Transparent,
                    darkIcons = false,
                )
            }
            if (event == Lifecycle.Event.ON_STOP) {
                systemUiController.setSystemBarsColor(systemBarsColor)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
