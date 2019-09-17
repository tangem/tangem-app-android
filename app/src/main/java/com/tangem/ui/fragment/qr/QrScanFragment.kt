package com.tangem.ui.fragment.qr

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.zxing.Result
import com.tangem.Constant
import com.tangem.ui.fragment.BaseFragment
import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScanFragment : BaseFragment(), ZXingScannerView.ResultHandler {

    override val layoutId = 0
    private var scannerView: ZXingScannerView? = null

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

    override fun handleResult(result: Result) {
        val data = Bundle()
        data.putString(Constant.EXTRA_QR_CODE, result.text)
        navigateBackWithResult(Activity.RESULT_OK, data)
    }

}