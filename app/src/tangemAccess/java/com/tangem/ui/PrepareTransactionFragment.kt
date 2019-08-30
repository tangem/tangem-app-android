package com.tangem.ui

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.UtilHelper
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.tangemAccess.fragment_prepare_transaction.*
import java.io.IOException

class PrepareTransactionFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback {
    companion object {
        val TAG: String = PrepareTransactionFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_prepare_transaction

    private val ctx: TangemContext by lazy { TangemContext.loadFromBundle(context, arguments) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
            if (!UtilHelper.isOnline(context!!)) {
                Toast.makeText(context, R.string.no_connection, Toast.LENGTH_LONG).show()
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

            val data = Bundle()
            ctx.saveToBundle(data)
            data.putString(Constant.EXTRA_TARGET_ADDRESS, etWallet!!.text.toString())
            data.putBoolean(Constant.EXTRA_FEE_INCLUDED, (rgIncFee!!.checkedRadioButtonId == R.id.rbFeeIn))
            data.putString(Constant.EXTRA_AMOUNT, strAmount)
            data.putString(Constant.EXTRA_AMOUNT_CURRENCY, tvCurrency.text.toString())
            navigateForResult(
                    Constant.REQUEST_CODE_SEND_TRANSACTION__,
                    R.id.action_prepareTransactionFragment_to_confirmTransactionFragment,
                    data)
        }

        ivCamera.setOnClickListener {
            navigateForResult(Constant.REQUEST_CODE_SCAN_QR, R.id.action_prepareTransactionFragment_to_qrScanFragment)
        }
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (requestCode == Constant.REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.containsKey("QRCode")) {
            var code = data.getString("QRCode")
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
                    } else if (code.contains("blockchain:")) { //TODO: is this needed?
                        val tmp = code.split("blockchain:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        code = tmp[1]
                    }
                }
                Blockchain.Litecoin -> {
                    if (code.contains("litecoin:")) {
                        val tmp = code.split("litecoin:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        code = tmp[1]
                    }
                }
                Blockchain.Ripple -> {
                    if (code.contains("ripple:")) {
                        val tmp = code.split("ripple:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        code = tmp[1]
                    }
                }
                else -> {
                }
            }
            etWallet?.setText(code)
        } else if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTION__) {
            navigateBackWithResult(resultCode, data)
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