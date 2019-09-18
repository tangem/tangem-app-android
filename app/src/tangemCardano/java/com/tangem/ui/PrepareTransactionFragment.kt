package com.tangem.ui

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.tangem.Constant
import com.tangem.data.dp.PrefsManager
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.event.TransactionFinishWithError
import com.tangem.ui.event.TransactionFinishWithSuccess
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.UtilHelper
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.tangemCardano.fragment_prepare_transaction.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.IOException

class PrepareTransactionFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback {

    override val layoutId = R.layout.fragment_prepare_transaction

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val engine = CoinEngineFactory.createCardano(TangemContext())

        @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(engine?.balanceHTML, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(engine?.balanceHTML)
        tvBalance.text = html

        if (!engine!!.allowSelectFeeInclusion()) {
            rgIncFee.visibility = View.INVISIBLE
        } else {
            rgIncFee.visibility = View.VISIBLE
        }

        tvCurrency.text = engine.balance?.currency
        etAmount.setText(engine.balance?.toValueString())

        // limit number of symbols after comma
        etAmount.filters = engine.amountInputFilters

        etWallet.setText(PrefsManager.getInstance().lastWalletAddress)

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
            if (!UtilHelper.isOnline(requireContext())) {
                Toast.makeText(context, R.string.general_error_no_connection, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // check on empty wallet address
            if (etWallet.text.toString() == "") {
                etWallet.error = getString(R.string.general_wallet_empty)
                return@setOnClickListener
            }

            // check on empty amount
            if (etAmount.text.toString() == "") {
                etAmount.error = getString(R.string.prepare_transaction_error_amount_empty)
                return@setOnClickListener
            }

            val strAmount: String = etAmount.text.toString().replace(",", ".")
            val amount = engine.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

            // check amount
            try {
                if (!engine.checkNewTransactionAmount(amount))
                    etAmount.error = getString(R.string.prepare_transaction_error_not_enough_funds)
                else
                    etAmount.error = null
            } catch (e: Exception) {
                etAmount.error = getString(R.string.prepare_transaction_error_unknown_amount_format)
            }

            // check wallet address
            if (!engine.validateAddress(etWallet.text.toString())) {
                etWallet.error = getString(R.string.prepare_transaction_error_incorrect_destination)
                return@setOnClickListener
            } else
                etWallet.error = null

            if (!etAmount.error.isNullOrEmpty() || !etWallet.error.isNullOrEmpty()) {
                return@setOnClickListener
            }

            PrefsManager.getInstance().saveLastWalletAddress(etWallet.text.toString())

            val data = Bundle()
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
            etWallet.text.clear()
            navigateForResult(Constant.REQUEST_CODE_SCAN_QR, R.id.action_prepareTransactionFragment_to_qrScanFragment)
        }

        etWallet.setOnTouchListener(OnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= etWallet.right - etWallet.compoundDrawables[2].bounds.width()) {
                    etWallet.text.clear()
                    return@OnTouchListener true
                }
            }
            false
        })
    }

    override fun onStart() {
        super.onStart()
        if (!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun onTransactionFinishWithSuccess(transactionFinishWithSuccess: TransactionFinishWithSuccess) {
        transactionFinishWithSuccess.message?.let { message ->
            (activity as MainActivity).toastHelper.showSnackbarSuccess(requireContext(), cl, message)
        }
    }

    @Subscribe
    fun onTransactionFinishWithError(transactionFinishWithError: TransactionFinishWithError) {
        transactionFinishWithError.message?.let { message ->
            (activity as MainActivity).toastHelper.showSnackbarError(requireContext(), cl, message)
        }
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (requestCode == Constant.REQUEST_CODE_SCAN_QR && resultCode == Activity.RESULT_OK && data != null && data.containsKey("QRCode")) {
            val code = data.getString("QRCode")
//            when (ctx.blockchain) {
//                Blockchain.Bitcoin -> {
//                    if (code.contains("bitcoin:")) {
//                        val tmp = code.split("bitcoin:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                        code = tmp[1]
//                    }
//                }
//                Blockchain.Ethereum, Blockchain.Token -> {
//                    if (code.contains("ethereum:")) {
//                        val tmp = code.split("ethereum:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                        code = tmp[1]
//                    } else if (code.contains("blockchain:")) {
//                        val tmp = code.split("blockchain:".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
//                        code = tmp[1]
//                    }
//                }
//                else -> {
//                }
//            }
            etWallet?.setText(code)
//        } else if (requestCode == Constant.REQUEST_CODE_SEND_TRANSACTION__) {
//            setResult(resultCode, data)
//            finish()
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