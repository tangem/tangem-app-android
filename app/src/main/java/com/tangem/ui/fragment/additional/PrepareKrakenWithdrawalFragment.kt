package com.tangem.ui.fragment.additional

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.network.Kraken
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_prepare_kraken_withdrawal.*
import java.io.IOException
import java.math.BigDecimal
import java.net.URI
import java.util.*

class PrepareKrakenWithdrawalFragment : BaseFragment(), NavigationResultListener,
        NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareKrakenWithdrawalFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_prepare_kraken_withdrawal

    private val ctx: TangemContext by lazy { TangemContext.loadFromBundle(context, arguments) }
    private val kraken: Kraken by lazy { Kraken(context) }

    private var fee: BigDecimal? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvKey.text = kraken.key
        tvSecret.text = kraken.secretDescription

        tvCardID.text = ctx.card!!.cidDescription
        tvWallet.text = ctx.coinData!!.wallet
        val engine = CoinEngineFactory.create(ctx)

        tvCurrency.text = engine!!.balanceCurrency

        etAmount.setText(engine.convertToAmount(engine.convertToInternalAmount(ctx.card!!.denomination)).toValueString())
        etAmount.filters = engine.amountInputFilters

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
        btnLoad.setOnClickListener {
            try {
                val strAmount: String = etAmount.text.toString().replace(",", ".")

                var dblAmount: Double = strAmount.toDouble()
                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.kraken_request_withdrawal)

                kraken.requestWithdrawInfo(ctx.blockchain.currency, dblAmount.toString(), ctx.coinData!!.wallet)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.unknown_amount_format)
            }

        }

//        ivCamera.setOnClickListener { navigator.showQrScanActivity(this, Constant.REQUEST_CODE_SCAN_QR) }

        ivRefreshBalance.setOnClickListener { doRequestBalance() }

        kraken.setBalanceListener { response ->
            if (response.error != null && response.error.isNotEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = Arrays.toString(response.error)
            } else {
                when (ctx.blockchain) {
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
                tvBalanceCurrency.text = ctx.blockchain.currency
                tvBalance.setTextColor(Color.BLACK)
                btnLoad.visibility = View.VISIBLE
            }
            rlProgressBar.visibility = View.INVISIBLE
        }
        kraken.setErrorListener { throwable ->
            throwable.printStackTrace()
            rlProgressBar.visibility = View.INVISIBLE
            tvError.visibility = View.VISIBLE
            tvError.text = throwable.message
        }
        kraken.setWithdrawalListener { response ->
            rlProgressBar.visibility = View.INVISIBLE
            if (response.error != null && response.error.isNotEmpty()) {
                tvError.visibility = View.VISIBLE
                tvError.text = Arrays.toString(response.error)
            } else {
                Toast.makeText(context, "Withdrawal successful!", Toast.LENGTH_LONG).show()
                navigateUp()
            }
        }
        kraken.setWithdrawalInfoListener { response ->
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
        val builder = AlertDialog.Builder(context)

        // Set a title for alert dialog
        builder.setTitle(R.string.please_confirm_withdraw)

        // Set a message for alert dialog
        builder.setMessage(String.format("Continue with fee %s %s?", fee!!.toString().trimEnd('0'), ctx.blockchain.currency))

        // On click listener for dialog buttons
        val dialogClickListener = DialogInterface.OnClickListener { _, which ->
            when (which) {
                DialogInterface.BUTTON_POSITIVE -> {
                    try {
                        val strAmount: String = etAmount.text.toString().replace(",", ".")

                        var dblAmount: Double = strAmount.toDouble()

                        dblAmount += fee!!.toDouble()

                        rlProgressBar.visibility = View.VISIBLE
                        tvProgressDescription.text = getString(R.string.kraken_request_withdrawal)

                        kraken.requestWithdraw(ctx.blockchain.currency, dblAmount.toString(), ctx.coinData!!.wallet)
                    } catch (e: Exception) {
                        etAmount.error = getString(R.string.unknown_amount_format)
                    }
                }
                DialogInterface.BUTTON_NEGATIVE -> {
                    Toast.makeText(context, R.string.operation_canceled, Toast.LENGTH_LONG).show()
                }
            }
        }

        // Set the alert dialog positive/yes button
        builder.setPositiveButton(R.string.yes, dialogClickListener)

        // Set the alert dialog negative/no button
        builder.setNegativeButton(R.string.no, dialogClickListener)


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

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (resultCode == Activity.RESULT_OK && data != null && data.containsKey("QRCode")) {
            when (requestCode) {
                Constant.REQUEST_CODE_SCAN_QR -> {
                    val uri = URI(data.getString("QRCode"))
                    val query = uri.query
                    val params = query.split("&")
                    for (param in params) {
                        if (param.startsWith("key=")) kraken.key = param.substring(4)
                        else if (param.startsWith("secret=")) kraken.secret = param.substring(7)
                    }
                    tvKey!!.text = kraken.key
                    tvSecret!!.text = kraken.secretDescription
                }
            }
            kraken.saveAccountInfo()
            doRequestBalance()
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            (activity as MainActivity).nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}