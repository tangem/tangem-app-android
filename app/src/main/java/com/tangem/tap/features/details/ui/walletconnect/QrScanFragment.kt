package com.tangem.tap.features.details.ui.walletconnect

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.Result
import com.otaliastudios.cameraview.CameraView
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.store

class QrScanFragment : Fragment(0), ScannerView.ResultHandler {

    // private var scannerView: ZXingScannerView? = null

    private var scannerView: ScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    store.dispatch(NavigationAction.PopBackTo())
                }
            },
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (!permissionIsGranted()) requestPermission()

        //TODO FIX ISSUE AND DELETE THIS WRAPPER
        scannerView = ScannerView(activity)

        // scannerView = ZXingScannerView(activity)
        return scannerView
    }

    override fun onPause() {
        super.onPause()
        try {
            scannerView?.stopCamera()
        } catch (ex: Exception) {
            Log.e("scannerView.stopCamera:", ex.toString())
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            scannerView?.setResultHandler(this)
            scannerView?.startCamera()
        } catch (ex: Exception) {
            Log.e("scannerView.stopCamera:", ex.toString())
        }
    }

    override fun onDestroy() {
        try {
            scannerView?.stopCameraPreview()
            scannerView?.stopCamera()
            scannerView?.destroyDrawingCache()
        } catch (ex: Exception) {
            Log.e("scannerView.stopCamera:", ex.toString())
        }
        super.onDestroy()
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
    override fun handleResult(var1: Result?) {
            try {
                scannerView?.stopCamera()
            } catch (ex: Exception) {
                Log.e("scannerView.stopCamera:", ex.toString())
            }
            store.dispatch(NavigationAction.PopBackTo())

            if (!var1?.text.isNullOrBlank()) {
                store.dispatch(WalletConnectAction.OpenSession(var1?.text?:"qr code error"))
            }

    }
}
