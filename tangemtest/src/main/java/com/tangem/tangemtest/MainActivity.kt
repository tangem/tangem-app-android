package com.tangem.tangemtest

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardManager
import com.tangem.tangem_sdk_new.DefaultCardManagerDelegate
import com.tangem.tangem_sdk_new.NfcLifecycleObserver
import com.tangem.tangem_sdk_new.nfc.NfcManager
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private val nfcManager = NfcManager()
    private val cardManagerDelegate: DefaultCardManagerDelegate = DefaultCardManagerDelegate(nfcManager.reader)
    private val cardManager = CardManager(nfcManager.reader, cardManagerDelegate)

    private lateinit var cid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcManager.setCurrentActivity(this)
        cardManagerDelegate.activity = this

        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        btn_scan?.setOnClickListener { _ ->
            cardManager.scanCard {
                if (it is TaskEvent.Event<ScanEvent>) {
                    when (it.data) {
                        is ScanEvent.OnReadEvent -> {
                            cid = (it.data as ScanEvent.OnReadEvent).result.cardId
                        }
                        is ScanEvent.OnVerifyEvent -> {
                            runOnUiThread {
                                tv_card_cid?.text = cid
                                btn_sign.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
        btn_sign?.setOnClickListener { _ ->
            cardManager.sign(
                    createSampleHashes(),
                    cid) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread { tv_card_cid?.text = cid + " used to sign sample hashes." }
                }
            }
        }
    }

    private fun createSampleHashes(): Array<ByteArray> {
        val hash1 = ByteArray(32)
        for (i in 0 until 32) {
            hash1[i] = 1
        }
        val hash2 = ByteArray(32)
        for (i in 0 until 32) {
            hash2[i] = 2
        }
        return arrayOf(hash1, hash2)
    }

    override fun onTagDiscovered(tag: Tag?) {
        nfcManager.onTagDiscovered(tag)
    }
}