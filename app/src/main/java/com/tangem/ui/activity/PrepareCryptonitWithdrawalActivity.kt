package com.tangem.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tangem.data.network.Cryptonit
import com.tangem.card_android.android.reader.NfcManager
import com.tangem.data.Blockchain
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.TangemContext
import com.tangem.card_android.android.nfc.NfcLifecycleObserver
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_cryptonit_withdrawal.*
import java.io.IOException

class PrepareCryptonitWithdrawalActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareCryptonitWithdrawalActivity::class.java.simpleName
    }

    private lateinit var ctx: TangemContext
    private lateinit var nfcManager: NfcManager

    private var cryptonit: Cryptonit? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_cryptonit_withdrawal)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        nfcManager = NfcManager(this, this)
        lifecycle.addObserver(NfcLifecycleObserver(nfcManager))

        cryptonit = Cryptonit(this)

        etUsername.setText(cryptonit!!.username)
        etPassword.setText(cryptonit!!.password)
        etFee.setText(cryptonit!!.fee)

        tvCardID.text = ctx.card!!.cidDescription
        tvWallet.text = ctx.coinData!!.wallet
        val engine = CoinEngineFactory.create(ctx)

        tvCurrency.text = engine!!.balanceCurrency
        tvFeeCurrency.text = engine.feeCurrency

        etAmount.setText(engine.convertToAmount(engine.convertToInternalAmount(ctx.card!!.denomination)).toValueString())
        etAmount.filters = engine.amountInputFilters

        etAmount.setOnEditorActionListener { lv, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = lv.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(lv.windowToken, 0)
                true
            } else
                false
        }

        when (ctx.blockchain) {
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
                val dblAmount: Double = strAmount.toDouble()
                cryptonit!!.fee = strFee

                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.cryptonit_request_withdrawal)

                cryptonit!!.requestWithdrawCoins(ctx.blockchain.currency, dblAmount, ctx.coinData!!.wallet)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }
        }

        ivRefreshBalance.setOnClickListener {
            cryptonit!!.username = etUsername.text.toString()
            cryptonit!!.password = etPassword.text.toString()
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
            if (response.success != null && response.success!!) {
                Toast.makeText(this, R.string.withdrawal_successful, Toast.LENGTH_LONG).show();
                finish()
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = response.errors.toString()
            }
        }
        btnLoad.visibility = View.INVISIBLE
        doRequestBalance()
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun doRequestBalance() {
        if (cryptonit!!.haveAccountInfo()) {
            rlProgressBar.visibility = View.VISIBLE
            tvProgressDescription.text = getString(R.string.cryptonit_request_balance)
            tvError.visibility = View.INVISIBLE
            cryptonit!!.requestBalance(ctx.blockchain.currency)
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = getString(R.string.cryptonit_not_enough_account_data)
        }
    }

}