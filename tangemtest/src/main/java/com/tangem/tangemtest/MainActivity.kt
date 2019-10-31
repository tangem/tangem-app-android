package com.tangem.tangemtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardManager
import com.tangem.CardManagerDelegate
import com.tangem.tangem_sdk_new.DefaultCardManagerDelegate
import com.tangem.tangem_sdk_new.NfcManager
import com.tangem.tangem_sdk_new.NfcReader
import com.tangem.tangem_sdk_new.postUI
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val nfcReader = NfcReader()
    private val nfcManager = NfcManager(nfcReader)
    private val cardManagerDelegate: CardManagerDelegate = DefaultCardManagerDelegate(this, nfcReader)
    private val cardManager = CardManager(nfcReader, cardManagerDelegate)
    private lateinit var cid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nfcManager.setCurrentActivity(this)

//        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        btn_scan?.setOnClickListener { _ ->
            cardManager.scanCard {
                if (it is TaskEvent.Event<ScanEvent>) {
                    when (it.data) {
                        is ScanEvent.OnReadEvent -> {
                            cid = (it.data as ScanEvent.OnReadEvent).result.cardId
                        }
                        is ScanEvent.OnVerifyEvent -> {
                            postUI {
                                tv_card_cid?.text = cid
                                btn_sign.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
        btn_sign?.setOnClickListener { _ ->
            val hash1 = ByteArray(32)
            for (i in 0 until 32) {
                hash1[i] = 1
            }
            val hash2 = ByteArray(32)
            for (i in 0 until 32) {
                hash2[i] = 2
            }
            cardManager.sign(
                    arrayOf(hash1, hash2),
                    cid) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) postUI { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> postUI { tv_card_cid?.text = cid + " used to sign sample hashes." }
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcManager.onResume()
    }


    override fun onPause() {
        super.onPause()
        nfcManager.onPause()
    }


    override fun onDestroy() {
        super.onDestroy()
        nfcManager.onDestroy()
    }
}