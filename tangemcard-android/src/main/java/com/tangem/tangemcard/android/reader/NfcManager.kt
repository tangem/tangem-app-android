package com.tangem.tangemcard.android.reader

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.tangem.tangemcard.android.presentation.dialog.NfcEnableDialog
import com.tangem.tangemcard.util.Log
import java.io.IOException

class NfcManager(private val activity: FragmentActivity, private val readerCallback: NfcAdapter.ReaderCallback) {
    companion object {
        val TAG: String = NfcManager::class.java.simpleName

        // reader mode flags: listen for type A (not B), skipping ndef check
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        private const val DELAY_PRESENCE = 1500

        private const val REQUEST_NFC_PERMISSIONS = 1
        private val PERMISSIONS_NFC = arrayOf(Manifest.permission.NFC)

        // checks if the app has NFC permission if the app does not has permission then the user will be prompted to grant permissions
        fun verifyPermissions(activity: Activity) {
            // check if we have write permission
            val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.NFC)

            if (permission != PackageManager.PERMISSION_GRANTED) {
                // we don't have permission so prompt the user
                ActivityCompat.requestPermissions(activity, PERMISSIONS_NFC, REQUEST_NFC_PERMISSIONS)
            }
        }
    }

    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)
    private var nfcEnableDialog: NfcEnableDialog? = null
    private val broadcomWorkaround = false
    internal var errorCount = 0
    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                val state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_ON)
                if (state == NfcAdapter.STATE_ON || state == NfcAdapter.STATE_TURNING_ON) {
                    Log.i(TAG, "state: $state , dialog: $nfcEnableDialog")
                    nfcEnableDialog?.dismiss()

                    if (state == NfcAdapter.STATE_ON)
                        enableReaderMode()

                } else {
                    if (nfcEnableDialog == null || !nfcEnableDialog!!.isVisible)
                        showNFCEnableDialog()
                }
            }
        }
    }

    fun onResume() {
        // register broadcast receiver
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity.registerReceiver(mBroadcastReceiver, filter)

        if (nfcAdapter == null || !nfcAdapter.isEnabled)
            showNFCEnableDialog()
        else
            enableReaderMode()
    }

    fun onPause() {
        activity.unregisterReceiver(mBroadcastReceiver)
        disableReaderMode()
    }

    fun onStop() {
        nfcEnableDialog?.dismiss()
    }

    @Throws(IOException::class)
    fun ignoreTag(tag: Tag) {
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        //            nfcAdapter.ignore(tag, 500, null, null);
        //        }else{
        val isoDep = IsoDep.get(tag)
        isoDep?.close()
        //        }
    }

    fun notifyReadResult(success: Boolean) {
        //        if (success) {
        //            errorCount = 0;
        //        } else {
        //            errorCount++;
        //        }
        //        if (errorCount >= 3) {
        //            disableReaderMode();
        //            nfcAdapter = null;
        ////            Toast.makeText(activity,"NFC restarted!",Toast.LENGTH_SHORT).show();
        //            activity.runOnUiThread(() -> {
        //                nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        //                enableReaderMode();
        //            });
        //        }
    }

    private fun showNFCEnableDialog() {
        nfcEnableDialog = NfcEnableDialog()
        nfcEnableDialog?.show(activity.supportFragmentManager, NfcEnableDialog.TAG)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun enableReaderMode() {
        val options = Bundle()
        if (broadcomWorkaround) {
            /* This is a work around for some Broadcom chipsets that does
             * the presence check by sending commands that interrupt the
             * processing of the ongoing command.
             */
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, DELAY_PRESENCE)
        }
        nfcAdapter!!.enableReaderMode(activity, readerCallback, READER_FLAGS, options)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

}