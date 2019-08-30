package com.tangem.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tangem.App
import com.tangem.Constant
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.data.dp.PrefsManager
import com.tangem.di.Navigator
import com.tangem.di.ToastHelper
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.TangemContext
import com.tangem.ui.event.TransactionFinishWithError
import com.tangem.ui.event.TransactionFinishWithSuccess
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.tangemCardano.activity_prepare_transaction.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.IOException
import javax.inject.Inject

class PrepareTransactionActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {
    companion object {
        val TAG: String = PrepareTransactionActivity::class.java.simpleName
        fun callingIntent(context: Context, ctx: TangemContext): Intent {
            val intent = Intent(context, PrepareTransactionActivity::class.java)
            ctx.saveToIntent(intent)
            return intent
        }
    }

    @Inject
    internal lateinit var navigator: Navigator
    @Inject
    internal lateinit var toastHelper: ToastHelper

    private lateinit var nfcManager: NfcManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_transaction)

        App.navigatorComponent.inject(this)
        App.toastHelperComponent.inject(this)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        val engine = CoinEngineFactory.createCardano(TangemContext())

        @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(engine?.balanceHTML, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(engine?.balanceHTML)
        tvBalance.text = html

        if (!engine!!.allowSelectFeeInclusion()) {
            rgIncFee.visibility = View.INVISIBLE
        } else {
            rgIncFee.visibility = View.VISIBLE
        }

        tvCurrency.text = engine.balance?.currency
        etAmount.setText(engine.balance?.toValueString())

        // limit number of symbols after comma
        etAmount.filters = engine.amountInputFilters

        etWallet.setText(PrefsManager.getInstance().lastWalletAddress)

        // set listeners
        etAmount.setOnEditorActionListener { lv, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = lv.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(lv.windowToken, 0)
                lv.clearFocus()
                true
            } else {
                false
            }
        }

        btnVerify.setOnClickListener {
            if (!UtilHelper.isOnline(this)) {
                Toast.makeText(this, R.string.general_error_no_connection, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // check on empty wallet address
            if (etWallet.text.toString() == "") {
                etWallet.error = getString(R.string.general_wallet_empty)
                return@setOnClickListener
            }

            // check on empty amount
            if (etAmount.text.toString() == "") {
                etAmount.error = getString(R.string.prepare_transaction_error_amount_empty)
                return@setOnClickListener
            }

            val strAmount: String = etAmount.text.toString().replace(",", ".")
            val amount = engine.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

            // check amount
            try {
                if (!engine.checkNewTransactionAmount(amount))
                    etAmount.error = getString(R.string.prepare_transaction_error_not_enough_funds)
                else
                    etAmount.error = null
            } catch (e: Exception) {
                etAmount.error = getString(R.string.prepare_transaction_error_unknown_amount_format)
            }

            // check wallet address
            if (!engine.validateAddress(etWallet.text.toString())) {
                etWallet.error = getString(R.string.prepare_transaction_error_incorrect_destination)
                return@setOnClickListener
            } else
                etWallet.error = null

            if (!etAmount.error.isNullOrEmpty() || !etWallet.error.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val intent = Intent(baseContext, ConfirmTransactionActivity::class.java)
            intent.putExtra(Constant.EXTRA_TARGET_ADDRESS, etWallet!!.text.toString())
            intent.putExtra(Constant.EXTRA_FEE_INCLUDED, (rgIncFee!!.checkedRadioButtonId == R.id.rbFeeIn))
            intent.putExtra(Constant.EXTRA_AMOUNT, strAmount)
            intent.putExtra(Constant.EXTRA_AMOUNT_CURRENCY, tvCurrency.text.toString())
            startActivityForResult(intent, Constant.REQUEST_CODE_SEND_TRANSACTION__)

            PrefsManager.getInstance().saveLastWalletAddress(etWallet.text.toString())
        }

        ivCamera.setOnClickListener {
            navigator.showQrScanActivity(this, Constant.REQUEST_CODE_SCAN_QR)
            etWallet.text.clear()
        }

        etWallet.setOnTouchListener(OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= etWallet.right - etWallet.compoundDrawables[2].bounds.width()) {
                    etWallet.text.clear()
                    return@OnTouchListener true
                }
            }
            false
        })
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onTransactionFinishWithSuccess(transactionFinishWithSuccess: TransactionFinishWithSuccess) {
        transactionFinishWithSuccess.message?.let { toastHelper.showSnackbarSuccess(this, cl, it) }
    }

    @Subscribe
    fun onTransactionFinishWithError(transactionFinishWithError: TransactionFinishWithError) {
        transactionFinishWithError.message?.let { toastHelper.showSnackbarError(this, cl, it) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode")) {
            var code = data.getStringExtra("QRCode")
//            when (ctx.blockchain) {
//                Blockchain.Bitcoin -> {
//                    if (code.contains("bitcoin:")) {
//                        val tmp = code.split("bitcoin:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                        code = tmp[1]
//                    }
//                }
//                Blockchain.Ethereum, Blockchain.Token -> {
//                    if (code.contains("ethereum:")) {
//                        val tmp = code.split("ethereum:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                        code = tmp[1]
//                    } else if (code.contains("blockchain:")) {
//                        val tmp = code.split("blockchain:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                        code = tmp[1]
//                    }
//                }
//                else -> {
//                }
//            }
            etWallet?.setText(code)
//        } else if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTION__) {
//            setResult(resultCode, data)
//            finish()
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}