package com.tangem.tap.features.details.ui.walletconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import androidx.activity.OnBackPressedCallback
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import by.kirich1409.viewbindingdelegate.viewBinding
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.cameraview.CameraView
import com.tangem.core.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.ui.walletconnect.dialogs.PreviewBinder
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutQrScanningBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class QrScanFragment : Fragment(R.layout.layout_qr_scanning) {

    private val binding: LayoutQrScanningBinding by viewBinding(LayoutQrScanningBinding::bind)

    private val binder = PreviewBinder()

    private var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var cameraExecutor: ExecutorService? = null

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

        cameraProviderFuture?.addListener(
            {
                val cameraProvider = cameraProviderFuture?.get()
                binder.bindPreview(
                    context = requireContext(),
                    binding = binding,
                    lifecycleOwner = this,
                    cameraProvider = requireNotNull(cameraProvider),
                    cameraExecutor = requireNotNull(cameraExecutor),
                    onScanned = { result ->
                        store.dispatch(NavigationAction.PopBackTo())
                        setFitSystemWindows(fit = false)
                        if (result.isNotBlank()) {
                            store.dispatch(WalletConnectAction.OpenSession(result))
                        }
                    },
                )
            },
            ContextCompat.getMainExecutor(requireContext()),
        )

        binding.overlay.post {
            binding.overlay.setViewFinder()
        }
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