package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import android.view.View
import com.tangem.data.network.Cryptonit_OtherAPI
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemCard
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_cryptonit_other_api_withdrawal.*
import java.io.IOException

class PrepareCryptonitOtherAPIWithdrawalActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareCryptonitOtherAPIWithdrawalActivity::class.java.simpleName

        private const val REQUEST_CODE_SCAN_QR_KEY = 1
        private const val REQUEST_CODE_SCAN_QR_SECRET = 2
        private const val REQUEST_CODE_SCAN_QR_USER_ID = 3
    }

    private var card: TangemCard? = null
    private var nfcManager: NfcManager? = null
    private var cryptonit: Cryptonit_OtherAPI? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_cryptonit_other_api_withdrawal)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra("UID"))
        card!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        cryptonit = Cryptonit_OtherAPI(this)

        tvKey.text = cryptonit!!.key
        tvUserID.text = cryptonit!!.userId
        tvSecret.text = cryptonit!!.secretDescription

        tvCardID.text = card!!.cidDescription
        tvWallet.text = card!!.wallet
        val engine = CoinEngineFactory.create(card!!.blockchain)

        when (card!!.blockchain) {
            Blockchain.Ethereum, Blockchain.EthereumTestNet -> {
                tvCurrency.text = engine.getBalanceCurrency(card)
            }
            Blockchain.Bitcoin, Blockchain.BitcoinTestNet, Blockchain.BitcoinCash, Blockchain.BitcoinCashTestNet -> {
                tvCurrency.text = card!!.blockchain.currency
            }
            else -> {
                tvCurrency.text = engine.getBalanceCurrency(card)
            }
        }

        etAmount.setText(engine.convertByteArrayToAmount(card!!, card!!.denomination))
        when (card!!.blockchain) {
            Blockchain.Bitcoin ->
                etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(5))
            Blockchain.BitcoinCash ->
                etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))
            Blockchain.Ethereum ->
                etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(18))
            else -> {
            }
        }

        // set listeners
        btnLoad.setOnClickListener {

            try {
                val strAmount: String = etAmount.text.toString().replace(",", ".")
//                if (!engine.checkAmount(card, strAmount))
//                    etAmount.error = getString(R.string.unknown_amount_format)
                var dblAmount: Double = strAmount.toDouble()

                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.cryptonit_request_withdrawal)

                cryptonit!!.requestCryptoWithdrawal(card!!.blockchain.currency, dblAmount.toString(), card!!.wallet)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }

            //Toast.makeText(this, strAmount, Toast.LENGTH_LONG).show()
//            val balance = engine.getBalanceLong(card)!! / (card!!.blockchain.multiplier / 1000.0)
//            if (etAmount.text.toString().replace(",", ".").toDouble() > balance) {
//                etAmount.error = getString(R.string.not_enough_funds_on_your_account)
//                return@setOnClickListener
//            }

        }
        ivCameraKey.setOnClickListener {
            val intent = Intent(baseContext, QrScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR_KEY)
        }
        ivCameraSecret.setOnClickListener {
            val intent = Intent(baseContext, QrScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR_SECRET)
        }
        ivCameraUserId.setOnClickListener {
            val intent = Intent(baseContext, QrScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR_USER_ID)
        }

        ivRefreshBalance.setOnClickListener { doRequestBalance() }

        cryptonit!!.setBalanceListener { response ->
            when (card!!.blockchain) {
                Blockchain.Ethereum, Blockchain.EthereumTestNet -> {
                    tvBalance.text = response.eth_available
                }
                Blockchain.Bitcoin, Blockchain.BitcoinTestNet, Blockchain.BitcoinCash, Blockchain.BitcoinCashTestNet -> {
                    tvBalance.text = response.btc_available
                }
                else -> {
                }
            }

            tvBalanceCurrency.text = card!!.blockchain.currency
            tvBalance.setTextColor(Color.BLACK)
            rlProgressBar.visibility = View.INVISIBLE
            btnLoad.isActivated = true
        }
        cryptonit!!.setErrorListener { throwable ->
            throwable.printStackTrace()
            rlProgressBar.visibility = View.INVISIBLE
            tvError.visibility = View.VISIBLE
            tvError.text = throwable.message
        }
        cryptonit!!.setWithdrawalListener { response ->
            rlProgressBar.visibility = View.INVISIBLE
            if (response.success != null && response.success!!) finish()
            else {
                tvError.visibility = View.VISIBLE
                tvError.text = response.reason!!.toString()
            }
        }
        btnLoad.isActivated = false
        doRequestBalance()
    }

    private fun doRequestBalance() {
        if (cryptonit!!.havaAccountInfo()) {
            rlProgressBar.visibility = View.VISIBLE
            tvProgressDescription.text = getString(R.string.cryptonit_request_balance)
            tvError.visibility = View.INVISIBLE
            cryptonit!!.requestBalance(card!!.blockchain.currency, "USD")
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = getString(R.string.cryptonit_not_enough_account_data)
        }
    }

//    private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
//        this.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//            }
//
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//            }
//
//            override fun afterTextChanged(editable: Editable?) {
//                afterTextChanged.invoke(editable.toString())
//            }
//        })
//    }

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
        if( resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode") ) {
            when(requestCode) {
                REQUEST_CODE_SCAN_QR_KEY -> {
                    cryptonit!!.key = data.getStringExtra("QRCode")
                    tvKey!!.text = cryptonit!!.key
                }
                REQUEST_CODE_SCAN_QR_SECRET -> {
                    cryptonit!!.secret = data.getStringExtra("QRCode")
                    tvSecret!!.text = cryptonit!!.secretDescription
                }
                REQUEST_CODE_SCAN_QR_USER_ID -> {
                    cryptonit!!.userId = data.getStringExtra("QRCode")
                    tvUserID!!.text = cryptonit!!.userId
                }
            }
            cryptonit!!.saveAccountInfo()
            doRequestBalance()
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