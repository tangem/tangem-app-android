package com.tangem.presentation.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QRScanActivity extends AppCompatActivity  implements ZXingScannerView.ResultHandler{

    private ZXingScannerView mScannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_qrscan);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("QRScanActivity","User hasn't granted permission to use camera");
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA}, 1);
        }else {
            runScanner();
        }
    }

    void runScanner()
    {
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.i("QRScanActivity","permission was granted");
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    runScanner();

                } else {
                    Log.e("QRScanActivity","permission denied");
                    setResult(Activity.RESULT_CANCELED);
                    finish();

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    @Override
    public void handleResult(Result result) {
        Intent data=new Intent();
        data.putExtra("QRCode", result.getText());
        setResult(Activity.RESULT_OK,  data);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if( mScannerView!=null ) mScannerView.stopCamera();   // Stop camera on pause
    }

    @Override
    protected void onResume() {
        super.onResume();
        if( mScannerView!=null ) mScannerView.startCamera();
    }
}
