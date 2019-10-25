package com.tangem.tangem_sdk_new

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class NfcManager(private val reader: NfcReader) : NfcAdapter.ReaderCallback {


    private var activity: FragmentActivity? = null
    private var nfcAdapter: NfcAdapter? = null
    private var readerCallback: NfcAdapter.ReaderCallback? = null
//    private var nfcEnableDialog: NfcEnableDialog? = null


    fun setCurrentActivity(activity: FragmentActivity, readerCallback: NfcAdapter.ReaderCallback) {
        this.activity = activity
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        this.readerCallback = readerCallback
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action ?: return
            if (action == NfcAdapter.ACTION_ADAPTER_STATE_CHANGED) {
                if (intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_ON) == NfcAdapter.STATE_ON)
                    enableReaderMode()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
//        reader.isoDep = IsoDep.get(tag)
        if (reader.readingActive) reader.onTagDiscovered(tag) else ignoreTag(tag)
    }


    fun onResume() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity?.registerReceiver(mBroadcastReceiver, filter)

        if (nfcAdapter == null || nfcAdapter?.isEnabled != true)
//            showNFCEnableDialog()
        else
            enableReaderMode()
    }

    fun onPause() {
        activity?.unregisterReceiver(mBroadcastReceiver)
        disableReaderMode()
    }

    fun onStop() {
//        nfcEnableDialog?.dismiss()
    }

    fun onDestroy() {
        activity = null
        nfcAdapter = null
        readerCallback = null
    }

//    private fun showNFCEnableDialog() {
//        nfcEnableDialog = NfcEnableDialog()
//        activity?.supportFragmentManager?.let { nfcEnableDialog?.show(it, NfcEnableDialog.TAG) }
//    }

    private fun enableReaderMode() {
        val options = Bundle()
        nfcAdapter?.enableReaderMode(activity, readerCallback, READER_FLAGS, options)
    }

    private fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    fun ignoreTag(tag: Tag?) {
        if (Build.VERSION.SDK_INT >= 24) {
            nfcAdapter?.ignore(tag, 1500, null, null)
        }
        IsoDep.get(tag)?.close()
    }

    companion object {
        // reader mode flags: listen for type A (not B), skipping ndef check
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    }

}