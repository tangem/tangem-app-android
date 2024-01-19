package com.tangem.feature.qrscanning

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.viewModels
import com.tangem.core.ui.components.SystemBarsEffect
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
        if (!it) parentFragmentManager.popBackStack()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.router = innerRouter
        cameraExecutor = Executors.newSingleThreadExecutor()
        requestCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        setFitSystemWindows(fit = true)
        cameraPermissionLauncher.unregister()
        cameraExecutor.shutdown()
    }

    @Composable
    override fun ScreenContent(modifier: Modifier) {
        SystemBarsEffect {
            setSystemBarsColor(Color.Transparent, darkIcons = false)
        }
        QrScanningContent(
            executor = { cameraExecutor },
            analyzer = { analyzer },
            uiState = viewModel.uiState,
        )
        setFitSystemWindows(fit = false)
    }

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

    private fun setFitSystemWindows(fit: Boolean) {
        activity?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, fit)
        }
    }

    companion object {

        fun create() = QrScanningFragment()
    }
}