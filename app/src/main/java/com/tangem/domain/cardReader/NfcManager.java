package com.tangem.domain.cardReader;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.tangem.presentation.dialog.NFCEnableDialog;

import java.io.IOException;

public class NfcManager {
    public static final String TAG = NfcManager.class.getSimpleName();

    // reader mode flags: listen for type A (not B), skipping ndef check
    private static final int READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK | NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS;

    private NfcAdapter mNfcAdapter;
    private NFCEnableDialog mEnableNfcDialog;
    private Activity mActivity;
    private NfcAdapter.ReaderCallback mReaderCallback;

    private boolean broadcomWorkaround = false;
    private static final int DELAY_PRESENCE = 1500;


    public NfcManager(Activity activity, NfcAdapter.ReaderCallback readerCallback) {
        mActivity = activity;
        mReaderCallback = readerCallback;
        mNfcAdapter = NfcAdapter.getDefaultAdapter(activity);
    }

    public void onResume() {
        // register broadcast receiver
        IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        mActivity.registerReceiver(mBroadcastReceiver, filter);

        if (mNfcAdapter == null || !mNfcAdapter.isEnabled()) {
            showNFCEnableDialog();

        } else {
            enableReaderMode();
        }
    }

    public void onPause() {
        mActivity.unregisterReceiver(mBroadcastReceiver);
        disableReaderMode();
    }

    public void onStop() {
        if (mEnableNfcDialog != null) {
            mEnableNfcDialog.dismiss();
        }
    }

    public void ignoreTag(Tag tag) throws IOException {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            mNfcAdapter.ignore(tag, 500, null, null);
//        }else{
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            isoDep.close();
        }
//        }
    }

    int errorCount = 0;

    public void notifyReadResult(boolean success) {
//        if (success) {
//            errorCount = 0;
//        } else {
//            errorCount++;
//        }
//        if (errorCount >= 3) {
//            disableReaderMode();
//            mNfcAdapter = null;
////            Toast.makeText(mActivity,"NFC restarted!",Toast.LENGTH_SHORT).show();
//            mActivity.runOnUiThread(() -> {
//                mNfcAdapter = NfcAdapter.getDefaultAdapter(mActivity);
//                enableReaderMode();
//            });
//        }
    }

    private void showNFCEnableDialog() {
        mEnableNfcDialog = new NFCEnableDialog();
        mEnableNfcDialog.show(mActivity.getFragmentManager(), NFCEnableDialog.TAG);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @SuppressWarnings("deprecation")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null)
                return;
            if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_ON);
                if (state == NfcAdapter.STATE_ON || state == NfcAdapter.STATE_TURNING_ON) {
                    Log.d(TAG, "state: " + state + " , dialog: " + mEnableNfcDialog);
                    if (mEnableNfcDialog != null) {
                        mEnableNfcDialog.dismiss();
                    }
                    if (state == NfcAdapter.STATE_ON) {
                        enableReaderMode();
                    }
                } else {
                    if (mEnableNfcDialog == null || !mEnableNfcDialog.isVisible()) {
                        showNFCEnableDialog();
                    }
                }
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void enableReaderMode() {
        Bundle options = new Bundle();
        if (broadcomWorkaround) {
            /* This is a work around for some Broadcom chipsets that does
             * the presence check by sending commands that interrupt the
             * processing of the ongoing command.
             */
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, DELAY_PRESENCE);
        }
        mNfcAdapter.enableReaderMode(mActivity, mReaderCallback, READER_FLAGS, options);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void disableReaderMode() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableReaderMode(mActivity);
        }
    }

    private static final int REQUEST_NFC_PERMISSIONS = 1;
    private static String[] PERMISSIONS_NFC = {
            Manifest.permission.NFC
    };

    //Checks if the app has NFC permission
    //If the app does not has permission then the user will be prompted to grant permissions
    public static void verifyPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.NFC);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_NFC,
                    REQUEST_NFC_PERMISSIONS
            );
        }
    }
}

