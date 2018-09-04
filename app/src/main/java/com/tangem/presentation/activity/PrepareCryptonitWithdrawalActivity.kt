package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import android.view.View
import android.widget.Toast
import com.tangem.data.network.Cryptonit
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemCard
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_cryptonit_withdrawal.*
import java.io.IOException

class PrepareCryptonitWithdrawalActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareCryptonitWithdrawalActivity::class.java.simpleName
    }

    private var useCurrencyX1000: Boolean = false
    private var card: TangemCard? = null
    private var nfcManager: NfcManager? = null
    private var cryptonit: Cryptonit? = null


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_cryptonit_withdrawal)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra("UID"))
        card!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        cryptonit = Cryptonit(this)

        etUsername.setText(cryptonit!!.username)
        etPassword.setText(cryptonit!!.password)
        etFee.setText(cryptonit!!.fee)

        tvCardID.text = card!!.cidDescription
        tvWallet.text = card!!.wallet
        val engine = CoinEngineFactory.create(card!!.blockchain)

        when (card!!.blockchain) {
            Blockchain.Ethereum -> {
                tvCurrency.text = engine.getBalanceCurrency(card)
                useCurrencyX1000 = false

            }
            Blockchain.Bitcoin, Blockchain.BitcoinCash -> {
                tvCurrency.text = "m" + card!!.blockchain.currency
                useCurrencyX1000 = true
            }
            else -> {
                tvCurrency.text = engine.getBalanceCurrency(card)
                useCurrencyX1000 = false
            }
        }
        tvFeeCurrency.text = tvCurrency.text

        etAmount.setText(engine.convertByteArrayToAmount(card!!, card!!.denomination))
        when (card!!.blockchain) {
            Blockchain.Bitcoin -> {
                etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(5))
                etFee.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(5))
            }
            Blockchain.BitcoinCash -> {
                etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))
                etFee.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(8))
            }
            Blockchain.Ethereum -> {
                etAmount.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(18))
                etFee.filters = arrayOf<InputFilter>(DecimalDigitsInputFilter(18))
            }
            else -> {
            }
        }

        // set listeners
        btnLoad.setOnClickListener {

            try {
                val strAmount: String = etAmount.text.toString().replace(",", ".")
                val strFee: String = etFee.text.toString().replace(",", ".")
                var dblAmount: Double = strAmount.toDouble()
                var dblFee: Double = strFee.toDouble()
                if (useCurrencyX1000){
                    dblAmount /= 1000.0
                    dblFee /= 1000.0
                }
                cryptonit!!.fee=strFee

                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.cryptonit_request_withdrawal)

                cryptonit!!.requestWithdrawCoins(card!!.blockchain.currency, dblAmount, card!!.wallet)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }
        }

        ivRefreshBalance.setOnClickListener {
            cryptonit!!.username=etUsername.text.toString()
            cryptonit!!.password=etPassword.text.toString()
            cryptonit!!.saveAccountInfo()
            doRequestBalance()
        }

        cryptonit!!.setBalanceListener { response ->
            tvBalance.text = response.results[0].balance.toString()
            tvBalanceCurrency.text = response.results[0].currency // card!!.blockchain.currency
            tvBalance.setTextColor(Color.BLACK)
            rlProgressBar.visibility = View.INVISIBLE
            btnLoad.visibility = View.VISIBLE
        }
        cryptonit!!.setErrorListener { throwable ->
            throwable.printStackTrace()
            rlProgressBar.visibility = View.INVISIBLE
            tvError.visibility = View.VISIBLE
            tvError.text = throwable.message
        }
        cryptonit!!.setWithdrawalListener { response ->
            rlProgressBar.visibility = View.INVISIBLE
            if (response.success != null && response.success!!){
                Toast.makeText(this,"Withdrawal successful!", Toast.LENGTH_LONG).show();
                finish()
            }
            else {
                tvError.visibility = View.VISIBLE
                tvError.text = response.errors.toString()
            }
        }
        btnLoad.visibility = View.INVISIBLE
        doRequestBalance()
    }

    private fun doRequestBalance() {
        if (cryptonit!!.haveAccountInfo()) {
            rlProgressBar.visibility = View.VISIBLE
            tvProgressDescription.text = getString(R.string.cryptonit_request_balance)
            tvError.visibility = View.INVISIBLE
            cryptonit!!.requestBalance(card!!.blockchain.currency)
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = getString(R.string.cryptonit_not_enough_account_data)
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

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager!!.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

}