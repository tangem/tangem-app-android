package com.tangem.ui.fragment

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.tangem.App
import com.tangem.Constant
import com.tangem.tangem_card.util.Util
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.event.TransactionFinishWithSuccess
import com.tangem.util.UtilHelper
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import org.greenrobot.eventbus.EventBus
import java.io.IOException

class SendTransactionFragment : BaseFragment(), NfcAdapter.ReaderCallback {

    override val layoutId = R.layout.fragment_send_transaction

    private lateinit var ctx: TangemContext

    private var tx: ByteArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)
        tx = arguments?.getByteArray(Constant.EXTRA_TX)

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
                        return UtilHelper.isOnline(requireContext())
                    }
                },
                tx
        )

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(context, R.string.please_wait, Toast.LENGTH_LONG).show()
            }

        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            (activity as MainActivity).nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun finishWithSuccess() {
        val transactionFinishWithSuccess = TransactionFinishWithSuccess()
        EventBus.getDefault().post(transactionFinishWithSuccess)

        val data = Bundle()
        data.putString(Constant.EXTRA_MESSAGE, getString(R.string.transaction_has_been_successfully_signed))
        navigateBackWithResult(Activity.RESULT_OK, data, R.id.loadedWalletFragment)
    }

    private fun finishWithError(message: String) {
        val data = Bundle()
        data.putString(Constant.EXTRA_MESSAGE, String.format(getString(R.string.try_again_failed_to_send_transaction), message))
        navigateBackWithResult(Activity.RESULT_CANCELED, data, R.id.loadedWalletFragment)
    }

}