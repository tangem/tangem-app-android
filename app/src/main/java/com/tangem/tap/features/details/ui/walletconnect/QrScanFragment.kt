package com.tangem.tap.features.details.ui.walletconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.cameraview.CameraView
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.common.qrCodeScan.MLKitBarcodeAnalyzer
import com.tangem.tap.common.qrCodeScan.ScanningResultListener
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutQrScanningBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScanFragment : Fragment(R.layout.layout_qr_scanning) {

    private val binding: LayoutQrScanningBinding by viewBinding(LayoutQrScanningBinding::bind)

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFitSystemWindows(fit = true)
        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.dispatch(NavigationAction.PopBackTo())
                }
            },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!permissionIsGranted()) requestPermission()

        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(requireContext()),)

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider?) {
        cameraProvider?.unbindAll()

        val preview: Preview = Preview.Builder()
            .build()

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(binding.cameraPreview.width, binding.cameraPreview.height))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        val orientationEventListener = object : OrientationEventListener(requireContext()) {

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

        class ScanningListener : ScanningResultListener {
            override fun onScanned(result: String) {
                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                imageAnalysis.clearAnalyzer()
                store.dispatch(NavigationAction.PopBackTo())
                setFitSystemWindows(fit = false)
                if (result.isNotBlank()) {
                    store.dispatch(WalletConnectAction.OpenSession(result))
                }
            }
        }

        val analyzer: ImageAnalysis.Analyzer = MLKitBarcodeAnalyzer(ScanningListener())

        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

        preview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
    }

    override fun onDestroy() {
        super.onDestroy()
        setFitSystemWindows(fit = false)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != CameraView.PERMISSION_REQUEST_CODE) return

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            store.dispatch(WalletConnectAction.NotifyCameraPermissionIsRequired)
            store.dispatch(NavigationAction.PopBackTo())
        }
    }

    private fun permissionIsGranted(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CameraView.PERMISSION_REQUEST_CODE)
    }

    private fun setFitSystemWindows(fit: Boolean) {
        activity?.window?.let {
            WindowCompat.setDecorFitsSystemWindows(it, fit)
        }
    }
}
