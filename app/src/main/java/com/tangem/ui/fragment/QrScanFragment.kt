package com.tangem.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.zxing.Result
import com.tangem.Constant
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScanFragment : BaseFragment(), ZXingScannerView.ResultHandler {
    companion object {
        fun callingIntent(context: Context): Intent {
            return Intent(context, QrScanFragment::class.java)
        }
    }

    override val layoutId = 0

    private var scannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ActivityCompat.checkSelfPermission(context!!, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(activity!!, arrayOf(Manifest.permission.CAMERA), 1)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)

                else {
                    navigateBackWithResult(Activity.RESULT_CANCELED)
                }
            }
        }
    }

    override fun handleResult(result: Result) {
        val data = Bundle()
        data.putString(Constant.EXTRA_QR_CODE, result.text)

        navigateBackWithResult(Activity.RESULT_OK, data)
    }

}