package com.tangem.presentation.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.InputFilter
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tangem.data.network.Kraken
import com.tangem.domain.cardReader.NfcManager
import com.tangem.domain.wallet.Blockchain
import com.tangem.domain.wallet.CoinEngineFactory
import com.tangem.domain.wallet.TangemCard
import com.tangem.util.DecimalDigitsInputFilter
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.activity_prepare_kraken_withdrawal.*
import java.io.IOException
import java.math.BigDecimal
import java.net.URI
import java.util.*

class PrepareKrakenWithdrawalActivity : AppCompatActivity(), NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareKrakenWithdrawalActivity::class.java.simpleName

        private const val REQUEST_CODE_SCAN_QR = 1
    }

    private var card: TangemCard? = null
    private var nfcManager: NfcManager? = null
    private var kraken: Kraken? = null
    private var fee: BigDecimal? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prepare_kraken_withdrawal)

        MainActivity.commonInit(applicationContext)

        nfcManager = NfcManager(this, this)

        card = TangemCard(intent.getStringExtra("UID"))
        card!!.loadFromBundle(intent.extras!!.getBundle("Card"))

        kraken = Kraken(this)

        tvKey.text = kraken!!.key
        tvSecret.text = kraken!!.secretDescription

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

        etAmount.setOnEditorActionListener{lv,actionId,event->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = lv.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(lv.windowToken, 0)
                lv.clearFocus()
                true
            }else {
                false
            }
        }

        // set listeners
        btnLoad.setOnClickListener {

            try {
                val strAmount: String = etAmount.text.toString().replace(",", ".")

                var dblAmount: Double = strAmount.toDouble()
                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.kraken_request_withdrawal)

                kraken!!.requestWithdrawInfo(card!!.blockchain.currency, dblAmount.toString(), card!!.wallet)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }

        }
        ivCamera.setOnClickListener {
            val intent = Intent(baseContext, QrScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_SCAN_QR)
        }

        ivRefreshBalance.setOnClickListener { doRequestBalance() }

        kraken!!.setBalanceListener { response ->
            if (response.error != null && response.error.isNotEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = Arrays.toString(response.error)
            } else {
                when (card!!.blockchain) {
                    Blockchain.Ethereum -> {
                        tvBalance.text = response.result.XETH.trimEnd('0')
                    }
                    Blockchain.Bitcoin -> {
                        tvBalance.text = response.result.XXBT.trimEnd('0')
                    }
                    Blockchain.BitcoinCash -> {
                        tvBalance.text = response.result.BCH.trimEnd('0')
                    }
                    else -> {
                        tvBalance.text = "???"
                    }
                }
                tvBalanceCurrency.text = card!!.blockchain.currency
                tvBalance.setTextColor(Color.BLACK)
                btnLoad.visibility = View.VISIBLE
            }
            rlProgressBar.visibility = View.INVISIBLE
        }
        kraken!!.setErrorListener { throwable ->
            throwable.printStackTrace()
            rlProgressBar.visibility = View.INVISIBLE
            tvError.visibility = View.VISIBLE
            tvError.text = throwable.message
        }
        kraken!!.setWithdrawalListener { response ->
            rlProgressBar.visibility = View.INVISIBLE
            if (response.error != null && response.error.isNotEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = Arrays.toString(response.error)
            } else {
                Toast.makeText(this,"Withdrawal successful!", Toast.LENGTH_LONG).show()
                finish()
            }
        }
        kraken!!.setWithdrawalInfoListener { response ->
            rlProgressBar.visibility = View.INVISIBLE
            if (response.error != null && response.error.isNotEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = Arrays.toString(response.error)
            } else {
                fee = BigDecimal(response.result.fee)
                showConfirmDialog()
            }
        }
        btnLoad.visibility = View.INVISIBLE
        doRequestBalance()
    }

    // Method to show an alert dialog with yes, no and cancel button
    private fun showConfirmDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog


        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this)

        // Set a title for alert dialog
        builder.setTitle("Please confirm withdraw")

        // Set a message for alert dialog
        builder.setMessage(String.format("Continue with fee %s %s?", fee!!.toString().trimEnd('0'),card!!.blockchain.currency))

        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    try {
                        val strAmount: String = etAmount.text.toString().replace(",", ".")

                        var dblAmount: Double = strAmount.toDouble()

                        dblAmount+=fee!!.toDouble()

                        rlProgressBar.visibility = View.VISIBLE
                        tvProgressDescription.text = getString(R.string.kraken_request_withdrawal)

                        //Toast.makeText(this, String.format("Withdraw %s!",dblAmount.toString()), Toast.LENGTH_LONG).show()
                        kraken!!.requestWithdraw(card!!.blockchain.currency, dblAmount.toString(), card!!.wallet)
                    } catch (e: Exception) {
                        etAmount.error = getString(R.string.unknown_amount_format)
                    }
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    Toast.makeText(this, "Operation canceled!", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Set the alert dialog positive/yes button
        builder.setPositiveButton("YES", dialogClickListener)

        // Set the alert dialog negative/no button
        builder.setNegativeButton("NO", dialogClickListener)


        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    private fun doRequestBalance() {
        if (kraken!!.haveAccountInfo()) {
            rlProgressBar.visibility = View.VISIBLE
            tvProgressDescription.text = getString(R.string.kraken_request_balance)
            tvError.visibility = View.INVISIBLE
            kraken!!.requestBalance()
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = getString(R.string.kraken_not_enough_account_data)
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
        if (resultCode == Activity.RESULT_OK && data != null && data.extras!!.containsKey("QRCode")) {
            when (requestCode) {
                REQUEST_CODE_SCAN_QR -> {
                    val uri = URI(data.getStringExtra("QRCode"))
                    val query = uri.query
                    val params = query.split("&")
                    for (param in params) {
                        if (param.startsWith("key=")) kraken!!.key = param.substring(4)
                        else if (param.startsWith("secret=")) kraken!!.secret = param.substring(7)
                    }
                    tvKey!!.text = kraken!!.key
                    tvSecret!!.text = kraken!!.secretDescription
                }
            }
            kraken!!.saveAccountInfo()
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