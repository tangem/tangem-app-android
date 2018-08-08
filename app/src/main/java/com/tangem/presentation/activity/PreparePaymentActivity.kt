package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Html
import android.text.InputFilter
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
    private var mCard: TangemCard? = null
    private var mNfcManager: NfcManager? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_payment)

        MainActivity.commonInit(applicationContext)

        mNfcManager = NfcManager(this, this)

        mCard = TangemCard(intent.getStringExtra("UID"))
        mCard!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        tvCardID.text = mCard!!.cidDescription
        val engine = CoinEngineFactory.create(mCard!!.blockchain)

        if (mCard!!.blockchain == Blockchain.Token) {
            val html = Html.fromHtml(engine!!.getBalanceWithAlter(mCard))
            tvBalance.text = html
        } else
            tvBalance.text = engine!!.getBalanceWithAlter(mCard)

        if (mCard!!.remainingSignatures < 2)
            etAmount!!.isEnabled = false

        if (mCard!!.blockchain == Blockchain.Ethereum || mCard!!.blockchain == Blockchain.EthereumTestNet) {
            tvCurrency.text = engine.getBalanceCurrency(mCard)
            useCurrency = false
            etAmount!!.setText(engine.getBalanceValue(mCard))
        } else if (mCard!!.blockchain == Blockchain.Bitcoin || mCard!!.blockchain == Blockchain.BitcoinTestNet) {
            val balance = engine.getBalanceLong(mCard)!! / (mCard!!.blockchain.multiplier / 1000.0)
            tvCurrency.text = "m" + mCard!!.blockchain.currency
            useCurrency = true
            val output = FormatUtil.DoubleToString(balance)
            etAmount!!.setText(output)
        } else if (mCard!!.blockchain == Blockchain.BitcoinCash || mCard!!.blockchain == Blockchain.BitcoinCashTestNet) {
            val balance = engine.getBalanceLong(mCard)!! / (mCard!!.blockchain.multiplier / 1000.0)
            tvCurrency.text = "m" + mCard!!.blockchain.currency
            useCurrency = true
            val output = FormatUtil.DoubleToString(balance)
            etAmount!!.setText(output)
        } else {
            tvCurrency.text = engine.getBalanceCurrency(mCard)
            useCurrency = false
            etAmount!!.setText(engine.getBalanceValue(mCard))
        }

        if (mCard!!.blockchain == Blockchain.Bitcoin)
            etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))

        if (mCard!!.blockchain == Blockchain.BitcoinCash)
            etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))

        if (mCard!!.blockchain == Blockchain.Ethereum)
            etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(18))

        // set listeners
        btnVerify.setOnClickListener {
            val strAmount: String = etAmount!!.text.toString().replace(",", ".")

            val engine1 = CoinEngineFactory.create(mCard!!.blockchain)

            try {
                if (!engine.checkAmount(mCard, etAmount!!.text.toString()))
                    etAmount!!.error = getString(R.string.not_enough_funds_on_your_card)
            } catch (e: Exception) {
                etAmount!!.error = getString(R.string.unknown_amount_format)
            }

            var checkAddress = false
            if (engine1 != null)
                checkAddress = engine1.validateAddress(etWallet!!.text.toString(), mCard)


            if (!checkAddress) {
                etWallet.error = getString(R.string.incorrect_destination_wallet_address)
                return@setOnClickListener
            }

            if (etWallet.text.toString() == mCard!!.wallet) {
                etWallet.error = getString(R.string.destination_wallet_address_equal_source_address)
                return@setOnClickListener
            }

            val intent = Intent(baseContext, ConfirmPaymentActivity::class.java)
            intent.putExtra("UID", mCard!!.uid)
            intent.putExtra("Card", mCard!!.asBundle)
            intent.putExtra("Wallet", etWallet!!.text.toString())
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
        mNfcManager!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mNfcManager!!.onPause()
    }

    public override fun onStop() {
        super.onStop()
        mNfcManager!!.onStop()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode")) {
            var code = data.getStringExtra("QRCode")
            if (code.contains("bitcoin:")) {
                val tmp = code.split("bitcoin:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                code = tmp[1]
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
            mNfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

}