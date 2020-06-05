package com.tangem.tangem_sdk_new.nfc

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.os.Bundle
import com.tangem.Log
import com.tangem.ReadingActiveListener
import com.tangem.tangem_sdk_new.ui.NfcEnableDialog

/**
 * Helps use NFC, leveraging Android NFC functionality.
 * Launches [NfcAdapter], manages it with [Activity] lifecycle,
 * enables and disables Nfc Reading Mode, receives NFC [Tag].
 */
class NfcManager : NfcAdapter.ReaderCallback, ReadingActiveListener {

    override var readingIsActive: Boolean = false
        set(value) {
            if (value) {
                disableReaderMode()
                enableReaderMode()
            }
            field = value
        }

    val reader = NfcReader()
    private var activity: Activity? = null
    private var nfcAdapter: NfcAdapter? = null
    private var nfcEnableDialog: NfcEnableDialog? = null

    fun setCurrentActivity(activity: Activity) {
        this.activity = activity
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
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
        Log.i(this::class.simpleName!!, "Nfc tag is discovered")
        if (readingIsActive) reader.onTagDiscovered(tag) else ignoreTag(tag)
    }


    fun onResume() {
        val filter = IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)
        activity?.registerReceiver(mBroadcastReceiver, filter)
        handleNfcEnabled(nfcAdapter?.isEnabled == true)
        reader.listener = this
    }

    private fun handleNfcEnabled(nfcEnabled: Boolean) {
        if (nfcEnabled) {
            enableReaderMode()
            nfcEnableDialog?.cancel()
        } else {
            nfcEnableDialog = NfcEnableDialog()
            activity?.let{ nfcEnableDialog?.show(it) }
        }
    }

    fun onPause() {
        activity?.unregisterReceiver(mBroadcastReceiver)
        disableReaderMode()
        reader.listener = null
    }

    fun onDestroy() {
        activity = null
        nfcAdapter = null
    }

    private fun enableReaderMode() {
        nfcAdapter?.enableReaderMode(activity, this, READER_FLAGS, Bundle())
    }

    private fun disableReaderMode() {
        nfcAdapter?.disableReaderMode(activity)
    }

    private fun ignoreTag(tag: Tag?) {
        Log.i(this::class.simpleName!!, "Nfc tag is ignored")
        if (Build.VERSION.SDK_INT >= 24) {
            nfcAdapter?.ignore(tag, 1500, null, null)
        }
        IsoDep.get(tag)?.close()
    }

    companion object {
        // reader mode flags: listen for type A (not B), skipping ndef check
        private const val READER_FLAGS = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
    }

}