package com.tangem.tap.features.details.ui.walletconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.google.zxing.Result
import com.otaliastudios.cameraview.CameraView
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScanFragment : Fragment(0), ZXingScannerView.ResultHandler {

    private var scannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                store.dispatch(NavigationAction.PopBackTo())
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (!permissionIsGranted()) requestPermission()

        scannerView = ZXingScannerView(activity)
        return scannerView
    }

    override fun onPause() {
        super.onPause()
        scannerView?.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        scannerView?.setResultHandler(this)
        scannerView?.startCamera()
    }

    override fun handleResult(result: Result) {
        store.dispatch(NavigationAction.PopBackTo())
        activity?.window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        if (!result.text.isNullOrBlank()) {
            store.dispatch(WalletConnectAction.OpenSession(result.text))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        if (requestCode != CameraView.PERMISSION_REQUEST_CODE) return

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            store.dispatch(WalletConnectAction.NotifyCameraPermissionIsRequired)
            store.dispatch(NavigationAction.PopBackTo())
        }
    }

    private fun permissionIsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraPermission =
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            cameraPermission == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), CameraView.PERMISSION_REQUEST_CODE)
    }
}
