package com.tangem.tap.common.qrCodeScan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.zxing.Result
import com.otaliastudios.cameraview.CameraView.PERMISSION_REQUEST_CODE
import com.tangem.tap.features.send.redux.AddressPayIdActionUI
import com.tangem.tap.store
import me.dm7.barcodescanner.zxing.ZXingScannerView

/**
[REDACTED_AUTHOR]
 */
class ScanQrCodeActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {
    private lateinit var mScannerView: ZXingScannerView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)

        if (!permissionIsGranted()) requestPermission()

    }

    override fun onResume() {
        super.onResume()
        mScannerView.setResultHandler(this)
        mScannerView.startCamera()
    }

    override fun onPause() {
        super.onPause()
        mScannerView.stopCamera()
    }

    override fun handleResult(result: Result) {
        store.dispatch(AddressPayIdActionUI.SetAddressOrPayId(result.text))
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode != PERMISSION_REQUEST_CODE) return

        if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            finish()
        }
    }

    private fun permissionIsGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            cameraPermission == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
    }
}