package com.tangem.tap.common.qrCodeScan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.cameraview.CameraView.PERMISSION_REQUEST_CODE
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PreviewBinder
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutQrScanningBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
[REDACTED_AUTHOR]
 */
class ScanQrCodeActivity : AppCompatActivity() {

    private val binding: LayoutQrScanningBinding by viewBinding(LayoutQrScanningBinding::bind)

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraExecutor: ExecutorService? = null

    private val binder = PreviewBinder()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)

        if (!permissionIsGranted()) requestPermission()

        setContentView(R.layout.layout_qr_scanning)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraExecutor = Executors.newSingleThreadExecutor()

        cameraProviderFuture?.addListener(
            {
                val cameraProvider = cameraProviderFuture?.get()
                binder.bindPreview(
                    context = this,
                    binding = binding,
                    lifecycleOwner = this,
                    cameraProvider = requireNotNull(cameraProvider),
                    cameraExecutor = requireNotNull(cameraExecutor),
                    onScanned = { result ->
                        setResult(SCAN_QR_REQUEST_CODE, Intent().apply { putExtra(SCAN_RESULT, result) })
                        finish()
                    },
                )
            },
            ContextCompat.getMainExecutor(this),
        )

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) return

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish()
        }
    }

    private fun permissionIsGranted(): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
    }

    companion object {
        const val SCAN_QR_REQUEST_CODE = 1001
        const val SCAN_RESULT = "scanResult"
    }
}