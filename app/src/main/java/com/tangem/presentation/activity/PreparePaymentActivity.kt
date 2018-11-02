package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemContext
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_payment.*
import java.io.IOException

class PreparePaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PreparePaymentActivity::class.java.simpleName

        private const val REQUEST_CODE_SCAN_QR = 1
        private const val REQUEST_CODE_SEND_PAYMENT = 2
    }

    private lateinit var ctx: TangemContext
    private var nfcManager: NfcManager? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_payment)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        tvCardID.text = ctx.card!!.cidDescription
        val engine = CoinEngineFactory.create(ctx)

        val html = Html.fromHtml(engine!!.balanceHTML)
        tvBalance.text = html

        //TODO - to engine
        if (ctx.card!!.blockchain == Blockchain.Token && engine.balance.currency!="ETH") {
            rgIncFee!!.visibility = View.INVISIBLE
        } else {
            rgIncFee!!.visibility = View.VISIBLE
        }

        if (ctx.card!!.remainingSignatures < 2)
            etAmount.isEnabled = false

        tvCurrency.text = engine.balance.currency
        etAmount.setText(engine.balance.toValueString())

        // limit number of symbols after comma
        etAmount.filters = engine.amountInputFilters

        // set listeners
        etAmount.setOnEditorActionListener { lv, actionId, event ->
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

            val engine1 = CoinEngineFactory.create(ctx)

            val strAmount: String = etAmount.text.toString().replace(",", ".")
            val amount = engine1.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

            try {
                if (!engine.checkNewTransactionAmount(amount))
                    etAmount.error = getString(R.string.not_enough_funds_on_your_card)
                else
                    etAmount.error = null
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }

            var checkAddress = false
            if (engine1 != null)
                checkAddress = engine1.validateAddress(etWallet.text.toString())

            // check wallet address
            if (!checkAddress) {
                etWallet.error = getString(R.string.incorrect_destination_wallet_address)
                return@setOnClickListener
            } else {
                etWallet.error = null
            }

            if (etWallet.text.toString() == ctx.card!!.wallet) {
                etWallet.error = getString(R.string.destination_wallet_address_equal_source_address)
                return@setOnClickListener
            }

            // check enough funds
            // TODO - double with engin.checkAmount
//            if (etAmount.text.toString().replace(",", ".").toDouble() > engine.getBalanceValue(card).replace(",", ".").toDouble()) {
//                etAmount.error = getString(R.string.not_enough_funds_on_your_card)
//                return@setOnClickListener
//            }
            if (!etAmount.error.isNullOrEmpty() || !etWallet.error.isNullOrEmpty()) {
                return@setOnClickListener
            }

            val intent = Intent(baseContext, ConfirmPaymentActivity::class.java)
            ctx.saveToIntent(intent)
            intent.putExtra(SignPaymentActivity.EXTRA_TARGET_ADDRESS, etWallet!!.text.toString())
            intent.putExtra(SignPaymentActivity.EXTRA_FEE_INCLUDED, (rgIncFee!!.checkedRadioButtonId == R.id.rbFeeIn))
            intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT, strAmount)
            intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT_CURRENCY, tvCurrency.text.toString())
            startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT)
        }
        ivCamera.setOnClickListener {
            val intent = Intent(baseContext, QrScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR)
        }
    }

    public override fun onResume() {
        super.onResume()
        nfcManager!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        nfcManager!!.onPause()
    }

    public override fun onStop() {
        super.onStop()
        nfcManager!!.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode")) {
            var code = data.getStringExtra("QRCode")
            when (ctx.card!!.blockchain) {
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
            etWallet!!.setText(code)
        } else if (requestCode == REQUEST_CODE_SEND_PAYMENT) {

            setResult(resultCode, data)
            finish()
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

}