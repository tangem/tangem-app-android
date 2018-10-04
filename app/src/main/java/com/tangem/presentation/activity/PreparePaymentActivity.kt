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
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemCard
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.util.FormatUtil
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_payment.*
import java.io.IOException

class PreparePaymentActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PreparePaymentActivity::class.java.simpleName

        private const val REQUEST_CODE_SCAN_QR = 1
        private const val REQUEST_CODE_SEND_PAYMENT = 2
    }

    private var useCurrency: Boolean = false
    private var card: TangemCard? = null
    private var nfcManager: NfcManager? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_payment)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra("UID"))
        card!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        tvCardID.text = card!!.cidDescription
        val engine = CoinEngineFactory.create(card!!.blockchain)

        if (card!!.blockchain == Blockchain.Token) {
            val html = Html.fromHtml(engine!!.getBalanceWithAlter(card))
            tvBalance.text = html
            rgIncFee!!.visibility = View.INVISIBLE
        } else {
            tvBalance.text = engine!!.getBalanceWithAlter(card)
            rgIncFee!!.visibility = View.VISIBLE
        }

        if (card!!.remainingSignatures < 2)
            etAmount.isEnabled = false

        // Ethereum EthereumTestNet
        if (card!!.blockchain == Blockchain.Ethereum || card!!.blockchain == Blockchain.EthereumTestNet) {
            tvCurrency.text = engine.getBalanceCurrency(card)
            useCurrency = false
            etAmount.setText(engine.getBalanceValue(card))
        }

        // Bitcoin BitcoinTestNet
        else if (card!!.blockchain == Blockchain.Bitcoin || card!!.blockchain == Blockchain.BitcoinTestNet) {
            val balance = engine.getBalanceLong(card)!! / (card!!.blockchain.multiplier)
            tvCurrency.text = card!!.blockchain.currency
            useCurrency = true
            val output = FormatUtil.DoubleToString(balance)
            etAmount.setText(output)
        }

        // BitcoinCash BitcoinCashTestNet
        else if (card!!.blockchain == Blockchain.BitcoinCash || card!!.blockchain == Blockchain.BitcoinCashTestNet) {
            val balance = engine.getBalanceLong(card)!! / (card!!.blockchain.multiplier)
            tvCurrency.text = card!!.blockchain.currency
            useCurrency = true
            val output = FormatUtil.DoubleToString(balance)
        } else {
            tvCurrency.text = engine.getBalanceCurrency(card)
            useCurrency = false
            etAmount.setText(engine.getBalanceValue(card))
        }

        if (card!!.blockchain == Blockchain.Bitcoin)
            etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))

        if (card!!.blockchain == Blockchain.BitcoinCash)
            etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))

        if (card!!.blockchain == Blockchain.Ethereum)
            etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(18))


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

        // set listeners
        btnVerify.setOnClickListener {
            val strAmount: String = etAmount.text.toString().replace(",", ".")

            val engine1 = CoinEngineFactory.create(card!!.blockchain)

            try {
                if (!engine.checkAmount(card, etAmount.text.toString()))
                    etAmount.error = getString(R.string.not_enough_funds_on_your_card)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }

            var checkAddress = false
            if (engine1 != null)
                checkAddress = engine1.validateAddress(etWallet.text.toString(), card)

            // check wallet address
            if (!checkAddress) {
                etWallet.error = getString(R.string.incorrect_destination_wallet_address)
                return@setOnClickListener
            }

            if (etWallet.text.toString() == card!!.wallet) {
                etWallet.error = getString(R.string.destination_wallet_address_equal_source_address)
                return@setOnClickListener
            }

            // check enough funds
            if (etAmount.text.toString().replace(",", ".").toDouble() > engine.getBalanceValue(card).replace(",", ".").toDouble()) {
                etAmount.error = getString(R.string.not_enough_funds_on_your_card)
                return@setOnClickListener
            }

            val intent = Intent(baseContext, ConfirmPaymentActivity::class.java)
            intent.putExtra("UID", card!!.uid)
            intent.putExtra("Card", card!!.asBundle)
            intent.putExtra("Wallet", etWallet!!.text.toString())
            intent.putExtra("IncFee", (rgIncFee!!.checkedRadioButtonId == R.id.rbFeeIn))
            intent.putExtra(SignPaymentActivity.EXTRA_AMOUNT, strAmount)
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
            when (card!!.blockchain) {
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
//            Log.w(javaClass.name, "Ignore discovered tag!")
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

}