package com.tangem.tangemtest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.CardManager
import com.tangem.commands.personalization.CardConfig
import com.tangem.tangem_sdk_new.extensions.init
import com.tangem.tangemtest.extensions.init
import com.tangem.tasks.ScanEvent
import com.tangem.tasks.TaskError
import com.tangem.tasks.TaskEvent
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var cardManager: CardManager
    private lateinit var cardId: String
    private lateinit var issuerData: ByteArray
    private lateinit var issuerDataSignature: ByteArray
    private var issuerDataCounter: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardManager = CardManager.init(this)

        btn_scan?.setOnClickListener { _ ->
            cardManager.scanCard { taskEvent ->
                when (taskEvent) {
                    is TaskEvent.Event -> {
                        when (taskEvent.data) {
                            is ScanEvent.OnReadEvent -> {
                                // Handle returned card data
                                cardId = (taskEvent.data as ScanEvent.OnReadEvent).card.cardId
                                runOnUiThread {
                                    tv_card_cid?.text = cardId
                                    btn_create_wallet.isEnabled = true
                                }
                            }
                            is ScanEvent.OnVerifyEvent -> {
                                //Handle card verification
                                runOnUiThread {
                                    tv_card_cid?.text = cardId
                                    btn_sign.isEnabled = true
                                    btn_read_issuer_data.isEnabled = true
                                    btn_read_issuer_extra_data.isEnabled = true
                                    btn_write_issuer_data.isEnabled = true
                                    btn_purge_wallet.isEnabled = true
                                    btn_create_wallet.isEnabled = true
                                }
                            }
                        }
                    }
                    is TaskEvent.Completion -> {
                        if (taskEvent.error != null) {
                            if (taskEvent.error is TaskError.UserCancelled) {
                                // Handle case when user cancelled manually
                            }
                            // Handle other errors
                        }
                        // Handle completion
                    }
                }
            }
        }
        btn_sign?.setOnClickListener { _ ->
            cardManager.sign(
                    createSampleHashes(),
                    cardId) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread { tv_card_cid?.text = cardId + "was used to sign sample hashes." }
                }
            }
        }
        btn_read_issuer_data?.setOnClickListener { _ ->
            cardManager.readIssuerData(cardId) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread {
                        btn_write_issuer_data.isEnabled = true
                        tv_card_cid?.text = it.data.issuerData.contentToString()
                        issuerData = it.data.issuerData
                        issuerDataSignature = it.data.issuerDataSignature
                    }
                }
            }
        }
        btn_write_issuer_data?.setOnClickListener { _ ->
            cardManager.writeIssuerData(
                    cardId,
                    issuerData,
                    issuerDataSignature) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread {
                        tv_card_cid?.text = it.data.cardId
                    }
                }
            }
        }
        btn_read_issuer_extra_data?.setOnClickListener { _ ->
            cardManager.readIssuerExtraData(cardId) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread {
                        issuerDataCounter = (it.data.issuerDataCounter ?: 0) + 1
                        btn_write_issuer_data.isEnabled = true
                        tv_card_cid?.text = "Read ${it.data.issuerData.size} bytes of data."
                    }
                }
            }
        }
        btn_purge_wallet?.setOnClickListener { _ ->
            cardManager.purgeWallet(
                    cardId) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread {
                        tv_card_cid?.text = it.data.status.name
                    }
                }
            }
        }
        btn_create_wallet?.setOnClickListener { _ ->
            cardManager.createWallet(
                    cardId) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread {
                        tv_card_cid?.text = it.data.status.name
                        btn_sign.isEnabled = true
                        btn_read_issuer_data.isEnabled = true
                        btn_purge_wallet.isEnabled = true
                        btn_create_wallet.isEnabled = false

                    }
                }
            }
        }
        btn_read_write_user_data?.setOnClickListener { startActivity(Intent(this, TestUserDataActivity::class.java)) }
        btn_personalize?.setOnClickListener { _ ->
            cardManager.personalize(CardConfig.init(application)) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread { tv_card_cid?.text = it.data.cardId }
                }
            }
        }
        btn_depersonalize?.setOnClickListener { _ ->
            cardManager.depersonalize(cardId) {
                when (it) {
                    is TaskEvent.Completion -> {
                        if (it.error != null) runOnUiThread { tv_card_cid?.text = it.error!!::class.simpleName }
                    }
                    is TaskEvent.Event -> runOnUiThread {
                        tv_card_cid?.text = "Depersonalized: ${it.data.success.toString()}"
                    }
                }
            }
        }
    }

    private fun createSampleHashes(): Array<ByteArray> {
        val hash1 = ByteArray(32) { 1 }
        val hash2 = ByteArray(32) { 2 }
        return arrayOf(hash1, hash2)
    }
}