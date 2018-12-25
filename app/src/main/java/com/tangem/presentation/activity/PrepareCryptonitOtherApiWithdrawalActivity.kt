package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.tangem.App
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.network.CryptonitOtherApi
import com.tangem.di.Navigator
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemContext
import com.tangem.tangemcard.android.reader.NfcManager
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_cryptonit_other_api_withdrawal.*
import java.io.IOException
import javax.inject.Inject

class PrepareCryptonitOtherApiWithdrawalActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareCryptonitOtherApiWithdrawalActivity::class.java.simpleName
    }

    private lateinit var ctx: TangemContext
    private var nfcManager: NfcManager? = null
    private var cryptonit: CryptonitOtherApi? = null

    @Inject
    internal lateinit var navigator: Navigator

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_cryptonit_other_api_withdrawal)

        App.getNavigatorComponent().inject(this)

        nfcManager = NfcManager(this, this)

        ctx = TangemContext.loadFromBundle(this, intent.extras)

        cryptonit = CryptonitOtherApi(this)

        tvKey.text = cryptonit!!.key
        tvUserID.text = cryptonit!!.userId
        tvSecret.text = cryptonit!!.secretDescription

        tvCardID.text = ctx.card!!.cidDescription
        tvWallet.text = ctx.coinData!!.wallet
        val engine = CoinEngineFactory.create(ctx)

        tvCurrency.text = engine!!.balanceCurrency

        etAmount.setText(engine.convertToAmount(engine.convertToInternalAmount(ctx.card!!.denomination)).toValueString())
        etAmount.filters = engine.amountInputFilters

        // set listeners
        btnLoad.setOnClickListener {
            try {
                val strAmount: String = etAmount.text.toString().replace(",", ".")
//                if (!engine.checkAmount(card, strAmount))
//                    etAmount.error = getString(R.string.unknown_amount_format)
                val dblAmount: Double = strAmount.toDouble()

                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.cryptonit_request_withdrawal)

                cryptonit!!.requestCryptoWithdrawal(ctx.blockchain.currency, dblAmount.toString(), ctx.coinData!!.wallet)
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
        ivCameraKey.setOnClickListener { navigator.showQrScanActivity(this, Constant.REQUEST_CODE_SCAN_QR_KEY) }

        ivCameraSecret.setOnClickListener { navigator.showQrScanActivity(this, Constant.REQUEST_CODE_SCAN_QR_SECRET) }

        ivCameraUserId.setOnClickListener { navigator.showQrScanActivity(this, Constant.REQUEST_CODE_SCAN_QR_USER_ID) }

        ivRefreshBalance.setOnClickListener { doRequestBalance() }

        cryptonit!!.setBalanceListener { response ->
            when (ctx.blockchain) {
                Blockchain.Ethereum, Blockchain.EthereumTestNet -> {
                    tvBalance.text = response.eth_available
                }
                Blockchain.Bitcoin, Blockchain.BitcoinTestNet, Blockchain.BitcoinCash -> {
                    tvBalance.text = response.btc_available
                }
                else -> {
                }
            }

            tvBalanceCurrency.text = ctx.blockchain.currency
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
            cryptonit!!.requestBalance(ctx.blockchain.currency, "USD")
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
        if (resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode")) {
            when (requestCode) {
                Constant.REQUEST_CODE_SCAN_QR_KEY -> {
                    cryptonit!!.key = data.getStringExtra("QRCode")
                    tvKey!!.text = cryptonit!!.key
                }
                Constant.REQUEST_CODE_SCAN_QR_SECRET -> {
                    cryptonit!!.secret = data.getStringExtra("QRCode")
                    tvSecret!!.text = cryptonit!!.secretDescription
                }
                Constant.REQUEST_CODE_SCAN_QR_USER_ID -> {
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