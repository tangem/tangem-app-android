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
import com.tangem.data.Blockchain
import com.tangem.data.dp.PrefsManager
import com.tangem.di.Navigator
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemContext
import com.tangem.util.UtilHelper
import com.tangem.wallet.R
import kotlinx.android.synthetic.tangemCardano.activity_prepare_transaction.*
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

        fun callingIntent(context: Context): Intent {
            val intent = Intent(context, PrepareTransactionActivity::class.java)
//            ctx.saveToIntent(intent)
            return intent
        }
    }

    @Inject
    internal lateinit var navigator: Navigator

    private lateinit var ctx: TangemContext
    private lateinit var nfcManager: NfcManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_transaction)

        App.navigatorComponent.inject(this)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        tvCardID.text = ctx.card?.cidDescription
        val engine = CoinEngineFactory.create(ctx)

        @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(engine!!.balanceHTML, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(engine!!.balanceHTML)
        tvBalance.text = html

        if (!engine.allowSelectFeeInclusion()) {
            rgIncFee.visibility = View.INVISIBLE
        } else {
            rgIncFee.visibility = View.VISIBLE
        }

        if (ctx.card!!.remainingSignatures < 2)
            etAmount.isEnabled = false

        tvCurrency.text = engine.balance.currency
        etAmount.setText(engine.balance.toValueString())

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
                Toast.makeText(this, R.string.no_connection, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val engine1 = CoinEngineFactory.create(ctx)
            val strAmount: String = etAmount.text.toString().replace(",", ".")
            val amount = engine1!!.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

            try {
                if (!engine.checkNewTransactionAmount(amount))
                    etAmount.error = getString(R.string.not_enough_funds_on_your_card)
                else
                    etAmount.error = null
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }

            // check wallet address
            if (!engine1.validateAddress(etWallet.text.toString())) {
                etWallet.error = getString(R.string.incorrect_destination_wallet_address)
                return@setOnClickListener
            } else
                etWallet.error = null

            if (etWallet.text.toString() == ctx.coinData!!.wallet) {
                etWallet.error = getString(R.string.destination_wallet_address_equal_source_address)
                return@setOnClickListener
            }

            if (!etAmount.error.isNullOrEmpty() || !etWallet.error.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val intent = Intent(baseContext, ConfirmTransactionActivity::class.java)
            ctx.saveToIntent(intent)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constant.REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode")) {
            var code = data.getStringExtra("QRCode")
            when (ctx.blockchain) {
                Blockchain.Bitcoin -> {
                    if (code.contains("bitcoin:")) {
                        val tmp = code.split("bitcoin:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        code = tmp[1]
                    }
                }
                Blockchain.Ethereum, Blockchain.Token -> {
                    if (code.contains("ethereum:")) {
                        val tmp = code.split("ethereum:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        code = tmp[1]
                    } else if (code.contains("blockchain:")) {
                        val tmp = code.split("blockchain:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        code = tmp[1]
                    }
                }
                else -> {
                }
            }
            etWallet?.setText(code)
        } else if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTION__) {
            setResult(resultCode, data)
            finish()
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