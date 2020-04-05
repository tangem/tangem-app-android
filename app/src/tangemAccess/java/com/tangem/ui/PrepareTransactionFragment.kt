package com.tangem.ui

import android.app.Activity
import android.content.Context
import android.net.Uri
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
import com.tangem.ui.fragment.qr.CameraPermissionManager
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.UtilHelper
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.tangemAccess.fragment_prepare_transaction.*
import java.io.IOException
import java.util.*

class PrepareTransactionFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback {
    companion object {
        val TAG: String = PrepareTransactionFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_prepare_transaction

    private val ctx: TangemContext by lazy { TangemContext.loadFromBundle(context, arguments) }
    private val cameraPermissionManager: CameraPermissionManager by lazy { CameraPermissionManager(this) }

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

        if (ctx.card!!.remainingSignatures < 2) {
            etAmount.isEnabled = false
        }

        if (ctx.card.remainingSignatures == 1) {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(R.string.prepare_transaction_warning_last_signature)
                    .setMessage(R.string.prepare_transaction_warning_send_full_amount)
                    .setPositiveButton(R.string.general_ok) { _, _ -> }
                    .create()
                    .show()
        }

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
                Toast.makeText(context, R.string.general_error_no_connection, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val engine1 = CoinEngineFactory.create(ctx)
            val strAmount: String = etAmount.text.toString().replace(",", ".")
            val amount = engine1!!.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

            try {
                if (!engine.checkNewTransactionAmount(amount))
                    etAmount.error = getString(R.string.prepare_transaction_error_not_enough_funds)
                else
                    etAmount.error = null
            } catch (e: Exception) {
                etAmount.error = getString(R.string.prepare_transaction_error_unknown_amount_format)
            }

            // check wallet address
            if (!engine1.validateAddress(etWallet.text.toString())) {
                etWallet.error = getString(R.string.prepare_transaction_error_incorrect_destination)
                return@setOnClickListener
            } else
                etWallet.error = null

            if (etWallet.text.toString() == ctx.coinData!!.wallet) {
                etWallet.error = getString(R.string.prepare_transaction_error_same_address)
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
            if (cameraPermissionManager.isPermissionGranted()) {
                navigateForResult(Constant.REQUEST_CODE_SCAN_QR, R.id.action_prepareTransactionFragment_to_qrScanFragment)
            } else {
                cameraPermissionManager.requirePermission()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermissionManager.handleRequestPermissionResult(requestCode, grantResults) {
            navigateForResult(Constant.REQUEST_CODE_SCAN_QR, R.id.action_prepareTransactionFragment_to_qrScanFragment)
        }
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (requestCode == Constant.REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.containsKey("QRCode")) {
            val code = data.getString("QRCode")
            val schemeSplit = code!!.split(":")
            when (schemeSplit.size) {
                2 -> {
                    if (ctx.blockchain.officialName.toLowerCase(Locale.ROOT).replace("\\s", "") == schemeSplit[0]) {
                        val uri = Uri.parse(schemeSplit[1])
                        etWallet?.setText(uri.path)
//                        val amount = uri.getQueryParameter("amount") //TODO: enable after redesign
//                        if (amount != null) {
//                            etAmount?.setText(amount)
//                            rgIncFee.check(R.id.rbFeeOut)
//                        }
                    } else if (ctx.blockchain == Blockchain.Ripple && schemeSplit[0] == "ripple") {
                        val uri = Uri.parse(schemeSplit[1])
                        etWallet?.setText(uri.path)
                    } else {
                        etWallet?.setText(code)
                    }
                }
                else -> {
                    etWallet?.setText(code)
                }
            }
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