package com.tangem.presentation.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity

import com.google.zxing.Result

import me.dm7.barcodescanner.zxing.ZXingScannerView

class QrScanActivity : AppCompatActivity(), ZXingScannerView.ResultHandler {

    private var scannerView: ZXingScannerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        else
            runScanner()
    }

    override fun onPause() {
        super.onPause()
        if (scannerView != null)
            scannerView!!.stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (scannerView != null)
            scannerView!!.startCamera()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    runScanner()
                else {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        }
    }

    override fun handleResult(result: Result) {
        val data = Intent()
        data.putExtra("QRCode", result.text)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun runScanner() {
        // programmatically initialize the scanner view
        scannerView = ZXingScannerView(this)
        setContentView(scannerView)
        // register ourselves as a handler for scan results.
        scannerView!!.setResultHandler(this)
        scannerView!!.startCamera()
    }

}