package com.tangem.ui

import android.app.Activity
import android.content.SharedPreferences
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.Html
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.os.bundleOf
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.tangem_card.data.TangemCard
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD
import com.tangem.tangem_sdk.data.EXTRA_TANGEM_CARD_UID
import com.tangem.tangem_sdk.data.loadFromBundle
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.fragment.pin.PinRequestFragment
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.UtilHelper
import com.tangem.wallet.CoinEngine
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.tangemAccess.fragment_confirm_transaction.*
import java.io.IOException
import java.util.*

class ConfirmTransactionFragment : BaseFragment(), NavigationResultListener, NfcAdapter.ReaderCallback {

    override val layoutId = R.layout.fragment_confirm_transaction

    private lateinit var sp: SharedPreferences
    private lateinit var ctx: TangemContext
    private lateinit var amount: CoinEngine.Amount

    private var isIncludeFee: Boolean = true
    private var requestPIN2Count = 0
    private var nodeCheck = true
    private var dtVerified: Date? = null

    private var blockchainCallbacks: CoinEngine.BlockchainRequestsCallbacks? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sp = PreferenceManager.getDefaultSharedPreferences(context)
        ctx = TangemContext.loadFromBundle(requireContext(), arguments)

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                navigateUp()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val engine = CoinEngineFactory.create(ctx)

        @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            Html.fromHtml(engine!!.balanceHTML, Html.FROM_HTML_MODE_LEGACY)
        else
            Html.fromHtml(engine!!.balanceHTML)
        tvBalance.text = html

        isIncludeFee = arguments?.getBoolean(Constant.EXTRA_FEE_INCLUDED, true) ?: true

        if (isIncludeFee)
            tvIncFee.setText(R.string.confirm_transaction_including_fee)
        else
            tvIncFee.setText(R.string.confirm_transaction_not_including_fee)

        amount = CoinEngine.Amount(arguments?.getString(Constant.EXTRA_AMOUNT) ?: "0",
                arguments?.getString(Constant.EXTRA_AMOUNT_CURRENCY) ?: "")

        if (engine.allowSelectFeeInclusion())
            tvIncFee.visibility = View.VISIBLE
        else
            tvIncFee.visibility = View.INVISIBLE

        if (ctx.card.blockchainID == Blockchain.Token.id) {
            // for Blockchain.Token limit decimals
            etAmount.setText(amount.toValueString(ctx.card.tokensDecimal))
        } else {
            // for others
            etAmount.setText(amount.toValueString())
        }

        tvCurrency.text = engine.balanceCurrency
        tvCurrency2.text = engine.feeCurrency
        tvCardID.text = ctx.card.cidDescription
        etWallet.setText(arguments?.getString(Constant.EXTRA_TARGET_ADDRESS))

        btnSend.visibility = View.INVISIBLE

        if (!engine.allowSelectFeeLevel()) {
            rgFee.visibility = View.INVISIBLE
        }

        etFee.isEnabled = sp.getBoolean(getString(R.string.pref_manual_editing_fee), false)

        // set listeners
        rgFee.setOnCheckedChangeListener { _, checkedId -> doSetFee(checkedId) }
        etFee.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                try {
                    val eqFee = engine.evaluateFeeEquivalent(etFee!!.text.toString())
                    tvFeeEquivalent.text = eqFee

                    if (!ctx.coinData!!.amountEquivalentDescriptionAvailable) {
                        tvFeeEquivalent.error = getString(R.string.confirm_transaction_error_service_unavailable)
                        tvCurrency2.visibility = View.GONE
                        tvFeeEquivalent.visibility = View.GONE
                    } else
                        tvFeeEquivalent.error = null

                    if (sp.getBoolean(getString(R.string.pref_manual_editing_fee), false))
                        (activity as MainActivity).toastHelper
                                .showSingleToast(context, getString(R.string.confirm_transaction_warning_risk_delaying))

                } catch (e: Exception) {
                    e.printStackTrace()
                    tvFeeEquivalent.text = ""
                }
            }

            override fun afterTextChanged(s: Editable) {

            }
        })
        btnSend.setOnClickListener {
            if (UtilHelper.isOnline(requireContext())) {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, -1)

                if (dtVerified == null || dtVerified!!.before(calendar.time)) {
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.confirm_transaction_error_data_is_outdated))
                    return@setOnClickListener
                }

                val engineCoin = CoinEngineFactory.create(ctx)

                if (engineCoin!!.isNeedCheckNode && !nodeCheck) {
                    Toast.makeText(context, getString(R.string.confirm_transaction_error_cannot_reach_node), Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                val txFee = engineCoin.convertToAmount(etFee.text.toString(), tvCurrency2.text.toString())
                val txAmount = engineCoin.convertToAmount(etAmount.text.toString(), tvCurrency.text.toString())

                if (!engineCoin.hasBalanceInfo()) {
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.confirm_transaction_error_cannot_check_balance))
                    return@setOnClickListener

                } else if (!engineCoin.isBalanceNotZero) {
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.general_wallet_empty))
                    return@setOnClickListener

                } else if (!engineCoin.isExtractPossible) {
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.confirm_transaction_error_incoming_transaction_unconfirmed))
                    return@setOnClickListener
                }

                if (!engineCoin.checkNewTransactionAmountAndFee(txAmount, txFee, isIncludeFee)) {
                    finishWithError(Activity.RESULT_CANCELED, getString(R.string.prepare_transaction_error_not_enough_funds))
                    return@setOnClickListener
                }

                requestPIN2Count = 0
                val data = Bundle()
                data.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
                ctx.saveToBundle(data)
                data.putBoolean(Constant.EXTRA_FEE_INCLUDED, isIncludeFee)
                navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_, R.id.action_confirmTransactionFragment_to_pinRequestFragment, data)
            } else
                Toast.makeText(context, getString(R.string.general_error_no_connection), Toast.LENGTH_SHORT).show()
        }

        progressBar.visibility = View.VISIBLE

        if (!navigatedBack) requestFee()
    }

    private fun requestFee() {
        val coinEngine = CoinEngineFactory.create(ctx)
        coinEngine!!.requestFee(
                object : CoinEngine.BlockchainRequestsCallbacks {
                    override fun onComplete(success: Boolean) {
                        if (success) {
                            progressBar?.visibility = View.INVISIBLE
                            dtVerified = Date()
                            doSetFee(rgFee?.checkedRadioButtonId ?: R.id.rbNormalFee)
                        } else {
                            finishWithError(Activity.RESULT_CANCELED, ctx.error)
                        }
                    }

                    override fun onProgress() {
                    }

                    override fun allowAdvance(): Boolean {
                        return UtilHelper.isOnline(requireContext())
                    }
                },
                etWallet.text.toString(),
                amount)
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        Log.d("LIFECYCLE", "NavigationResult assessed ${this::class.java.simpleName}")
        if (requestCode == Constant.REQUEST_CODE_SIGN_TRANSACTION) {
            if (data != null) {
                if (data.containsKey(EXTRA_TANGEM_CARD_UID) && data.containsKey(EXTRA_TANGEM_CARD)) {
                    val updatedCard = TangemCard(data.getString(EXTRA_TANGEM_CARD_UID))
                    updatedCard.loadFromBundle(data.getBundle(EXTRA_TANGEM_CARD))
                    ctx.card = updatedCard
                }
            }
            if (resultCode == Constant.RESULT_INVALID_PIN_ && requestPIN2Count < 2) {
                requestPIN2Count++
                val bundle = Bundle()
                bundle.putString(Constant.EXTRA_MODE, PinRequestFragment.Mode.RequestPIN2.toString())
                ctx.saveToBundle(bundle)
                bundle.putBoolean(Constant.EXTRA_FEE_INCLUDED, isIncludeFee)
                navigateForResult(Constant.REQUEST_CODE_REQUEST_PIN2_,
                        R.id.action_confirmTransactionFragment_to_pinRequestFragment,
                        bundle)
                return
            }
            navigateBackWithResult(resultCode, data)
        } else if (requestCode == Constant.REQUEST_CODE_REQUEST_PIN2_) {
            if (resultCode == Activity.RESULT_OK) {
                val bundle = Bundle()
                ctx.saveToBundle(bundle)
                bundle.putString(Constant.EXTRA_TARGET_ADDRESS, etWallet!!.text.toString())
                bundle.putString(Constant.EXTRA_AMOUNT, etAmount.text.toString())
                bundle.putString(Constant.EXTRA_AMOUNT_CURRENCY, tvCurrency.text.toString())
                bundle.putString(Constant.EXTRA_FEE, etFee.text.toString())
                bundle.putString(Constant.EXTRA_FEE_CURRENCY, tvCurrency2.text.toString())
                bundle.putBoolean(Constant.EXTRA_FEE_INCLUDED, isIncludeFee)
                navigateForResult(Constant.REQUEST_CODE_SIGN_TRANSACTION,
                        R.id.action_confirmTransactionFragment_to_signTransactionFragment,
                        bundle)
            } else
                Toast.makeText(context, R.string.confirm_transaction_error_pin_2_is_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            (activity as MainActivity).nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun doSetFee(checkedRadioButtonId: Int) {
        var txtFee = ""
        when (checkedRadioButtonId) {
            R.id.rbMinimalFee ->
                if (ctx.coinData.minFee != null) {
                    txtFee = ctx.coinData.minFee!!.toValueString()
                    btnSend?.visibility = View.VISIBLE
                } else
                    btnSend?.visibility = View.INVISIBLE

            R.id.rbNormalFee ->
                if (ctx.coinData.normalFee != null) {
                    txtFee = ctx.coinData.normalFee!!.toValueString()
                    btnSend?.visibility = View.VISIBLE
                } else
                    btnSend?.visibility = View.INVISIBLE

            R.id.rbMaximumFee ->
                if (ctx.coinData.maxFee != null) {
                    txtFee = ctx.coinData.maxFee!!.toValueString()
                    btnSend?.visibility = View.VISIBLE
                } else
                    btnSend?.visibility = View.INVISIBLE
        }
        etFee?.setText(txtFee.replace(',', '.'))
    }

    private fun finishWithError(errorCode: Int, message: String) {
        navigateBackWithResult(
                errorCode,
                bundleOf(Constant.EXTRA_MESSAGE to message),
                R.id.loadedWalletFragment)
    }
}