package com.tangem.tangemtest

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tangem.TangemSdk
import com.tangem.common.CompletionResult
import com.tangem.tangem_sdk_new.extensions.init
import kotlinx.android.synthetic.main.old_activity_main.*

class Old_MainActivity : AppCompatActivity() {

    private lateinit var tangemSdk: TangemSdk
    private lateinit var cardId: String
    private lateinit var issuerData: ByteArray
    private lateinit var issuerDataSignature: ByteArray
    private var issuerDataCounter: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.old_activity_main)

        tangemSdk = TangemSdk.init(this)

        btn_scan?.setOnClickListener { _ ->
            tangemSdk.scanCard { taskEvent ->
                when (taskEvent) {
                    is CompletionResult.Success -> {
                        // Handle returned card data
                        val card = taskEvent.data
                        cardId = card.cardId
                        runOnUiThread {
                            tv_card_cid?.text = cardId
                            btn_create_wallet.isEnabled = true
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
        }
        btn_sign?.setOnClickListener { _ ->
            tangemSdk.sign(
                    createSampleHashes(),
                    cardId) {
                when (it) {
                    is CompletionResult.Failure -> {
                        runOnUiThread { tv_card_cid?.text = it.error::class.simpleName }
                    }
                    is CompletionResult.Success -> runOnUiThread { tv_card_cid?.text = cardId + "was used to sign sample hashes." }
                }
            }
        }
        btn_read_issuer_data?.setOnClickListener { _ ->
            tangemSdk.readIssuerData(cardId) {
                when (it) {
                    is CompletionResult.Failure -> {
                        runOnUiThread { tv_card_cid?.text = it.error::class.simpleName }
                    }
                    is CompletionResult.Success -> runOnUiThread {
                        btn_write_issuer_data.isEnabled = true
                        tv_card_cid?.text = it.data.issuerData.contentToString()
                        issuerData = it.data.issuerData
                        issuerDataSignature = it.data.issuerDataSignature
                    }
                }
            }
        }
        btn_write_issuer_data?.setOnClickListener { _ ->
            tangemSdk.writeIssuerData(
                    cardId,
                    issuerData,
                    issuerDataSignature) {
                when (it) {
                    is CompletionResult.Failure -> {
                        runOnUiThread { tv_card_cid?.text = it.error::class.simpleName }
                    }
                    is CompletionResult.Success -> runOnUiThread {
                        tv_card_cid?.text = it.data.cardId
                    }
                }
            }
        }
        btn_read_issuer_extra_data?.setOnClickListener { _ ->
            tangemSdk.readIssuerExtraData(cardId) {
                when (it) {
                    is CompletionResult.Failure -> {
                        runOnUiThread { tv_card_cid?.text = it.error::class.simpleName }
                    }
                    is CompletionResult.Success -> runOnUiThread {
                        issuerDataCounter = (it.data.issuerDataCounter ?: 0) + 1
                        btn_write_issuer_data.isEnabled = true
                        tv_card_cid?.text = "Read ${it.data.issuerData.size} bytes of data."
                    }
                }
            }
        }
        btn_purge_wallet?.setOnClickListener { _ ->
            tangemSdk.purgeWallet(
                    cardId) {
                when (it) {
                    is CompletionResult.Failure -> {
                        runOnUiThread { tv_card_cid?.text = it.error::class.simpleName }
                    }
                    is CompletionResult.Success -> runOnUiThread {
                        tv_card_cid?.text = it.data.status.name
                    }
                }
            }
        }
        btn_create_wallet?.setOnClickListener { _ ->
            tangemSdk.createWallet(
                    cardId) {
                when (it) {
                    is CompletionResult.Failure -> {
                        runOnUiThread { tv_card_cid?.text = it.error::class.simpleName }
                    }
                    is CompletionResult.Success -> runOnUiThread {
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
    }

    private fun createSampleHashes(): Array<ByteArray> {
        val hash1 = ByteArray(32) { 1 }
        val hash2 = ByteArray(32) { 2 }
        return arrayOf(hash1, hash2)
    }
}