package com.tangem.ui.activity

import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.KeyEvent
import android.widget.Toast
import com.tangem.App
import com.tangem.Constant
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.TangemContext
import com.tangem.ui.event.TransactionFinishWithError
import com.tangem.ui.event.TransactionFinishWithSuccess
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.card_common.util.Util
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import org.greenrobot.eventbus.EventBus
import java.io.IOException

class SendTransactionActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    private lateinit var nfcManager: NfcManager
    private lateinit var ctx: TangemContext

    private var tx: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_transaction)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        ctx = TangemContext.loadFromBundle(this, intent.extras)
        tx = intent.getByteArrayExtra(Constant.EXTRA_TX)

        val engine = CoinEngineFactory.create(ctx)

        engine!!.requestSendTransaction(
                object : CoinEngine.BlockchainRequestsCallbacks {
                    override fun onComplete(success: Boolean) {
                        if (success) {
                            App.pendingTransactionsStorage.putTransaction(ctx.card, Util.bytesToHex(tx), engine.pendingTransactionTimeoutInSeconds())
                            finishWithSuccess()
                        }else
                            finishWithError(ctx.error)
                    }

                    override fun onProgress() {
                    }

                    override fun allowAdvance(): Boolean {
                        return UtilHelper.isOnline(this@SendTransactionActivity)
                    }
                },
                tx
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                Toast.makeText(this, R.string.please_wait, Toast.LENGTH_LONG).show()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun finishWithSuccess() {
        val transactionFinishWithSuccess = TransactionFinishWithSuccess()
        transactionFinishWithSuccess.message = getString(R.string.transaction_has_been_successfully_signed)
        EventBus.getDefault().post(transactionFinishWithSuccess)

        val intent = Intent()
        intent.putExtra(Constant.EXTRA_MESSAGE, getString(R.string.transaction_has_been_successfully_signed))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun finishWithError(message: String) {
        val transactionFinishWithError = TransactionFinishWithError()
        transactionFinishWithError.message = String.format(getString(R.string.try_again_failed_to_send_transaction), message)
        EventBus.getDefault().post(transactionFinishWithError)

        val intent = Intent()
        intent.putExtra(Constant.EXTRA_MESSAGE, String.format(getString(R.string.try_again_failed_to_send_transaction), message))
        setResult(RESULT_CANCELED, intent)
        finish()
    }

}