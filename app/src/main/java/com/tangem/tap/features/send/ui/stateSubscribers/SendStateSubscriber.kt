package com.tangem.tap.features.send.ui.stateSubscribers

import android.app.Dialog
import android.content.Context
import android.text.InputType
import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.getMessageString
import com.tangem.tap.common.text.DecimalDigitsInputFilter
import com.tangem.tap.domain.MultiMessageError
import com.tangem.tap.domain.assembleErrors
import com.tangem.tap.features.BaseStoreFragment
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.Error
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.SendAction
import com.tangem.tap.features.send.redux.reducers.ReceiptReducer
import com.tangem.tap.features.send.redux.states.*
import com.tangem.tap.features.send.ui.FeeUiHelper
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.tap.features.send.ui.dialogs.SendTransactionFailsDialog
import com.tangem.tap.features.send.ui.dialogs.TezosWarningDialog
import com.tangem.tap.features.wallet.ui.adapters.WarningMessagesAdapter
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.btn_expand_collapse.*
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.fragment_send.clReceiptContainer
import kotlinx.android.synthetic.main.layout_receipt_total.*
import kotlinx.android.synthetic.main.layout_receipt_total.view.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_amount.*
import kotlinx.android.synthetic.main.layout_send_fee.*
import kotlinx.android.synthetic.main.layout_send_receipt.*

/**
[REDACTED_AUTHOR]
 */
class SendStateSubscriber(fragment: BaseStoreFragment) : FragmentStateSubscriber<SendState>(fragment) {

    private var dialog: Dialog? = null

    override fun updateWithNewState(fg: BaseStoreFragment, state: SendState) {
        val lastChangedStates = state.lastChangedStates.toList()
        state.lastChangedStates.clear()
        fg.main_send_container.beginDelayedTransition()
        lastChangedStates.forEach {
            when (it) {
                StateId.SEND_SCREEN -> handleSendScreen(fg, state)
                StateId.ADDRESS_PAY_ID -> handleAddressPayIdState(fg, state.addressPayIdState)
                StateId.TRANSACTION_EXTRAS -> handleTransactionExtrasState(fg, state.transactionExtrasState)
                StateId.AMOUNT -> handleAmountState(fg, state.amountState)
                StateId.FEE -> handleFeeState(fg, state.feeState)
                StateId.RECEIPT -> handleReceiptState(fg, state.receiptState)
            }
        }
    }

    private fun handleTransactionExtrasState(fg: BaseStoreFragment, infoState: TransactionExtrasState) {
        fun showView(view: View, info: Any?) {
            view.show(info != null)
        }
        showView(fg.xlmMemoContainer, infoState.xlmMemo)
        showView(fg.xrpDestinationTagContainer, infoState.xrpDestinationTag)

        infoState.xlmMemo?.let {
            fg.etMemo.inputType = when (it.selectedMemoType) {
                XlmMemoType.TEXT -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                XlmMemoType.ID -> InputType.TYPE_CLASS_NUMBER
            }
            if (!it.viewFieldValue.isFromUserInput) fg.etMemo.setText(it.viewFieldValue.value)
            if (it.error != null) {
                if (it.error == TransactionExtraError.INVALID_XLM_MEMO) {
                    fg.tilMemo.error = fg.getText(R.string.send_error_invalid_memo_id)
                }
            } else {
                fg.tilMemo.error = null
            }
        }
        infoState.xrpDestinationTag?.let {
            if (infoState.xrpDestinationTag.error != null) {
                if (infoState.xrpDestinationTag.error == TransactionExtraError.INVALID_DESTINATION_TAG) {
                    fg.tilDestinationTag.error = fg.getText(R.string.send_error_invalid_destination_tag)
                }
            } else {
                fg.tilDestinationTag.error = null
            }
            if (!it.viewFieldValue.isFromUserInput) {
                fg.etDestinationTag.setText(it.viewFieldValue.value)
            }
        }
    }

    private fun handleSendScreen(fg: BaseStoreFragment, state: SendState) {
        val sendFragment = (fg as? SendFragment) ?: return

        when (state.dialog) {
            is SendAction.Dialog.TezosWarningDialog -> {
                if (dialog == null) {
                    dialog = TezosWarningDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            is SendAction.Dialog.SendTransactionFails -> {
                if (dialog == null) {
                    dialog = SendTransactionFailsDialog.create(fg.requireContext(), state.dialog)
                    dialog?.show()
                }
            }
            else -> {
                dialog?.dismiss()
                dialog = null
            }
        }

        sendFragment.sendBtn.changeState(state.sendButtonState.progressState)
        sendFragment.sendBtn.mainView.isEnabled = state.sendButtonState.enabled

        val rv = fg.rv_warning_messages
        val adapter = rv.adapter as? WarningMessagesAdapter ?: return

        adapter.submitList(state.sendWarningsList)
        rv.show(state.sendWarningsList.isNotEmpty())
    }

    private fun handleAddressPayIdState(fg: BaseStoreFragment, state: AddressPayIdState) {
        fun parseError(context: Context, error: Error?): String? {
            val resId = when (error) {
                Error.PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.send_error_payid_unsupported_by_blockchain
                Error.PAY_ID_NOT_REGISTERED -> R.string.send_error_payid_not_registered
                Error.PAY_ID_REQUEST_FAILED -> R.string.send_error_payid_request_failed
                Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.send_validation_invalid_address
                Error.ADDRESS_SAME_AS_WALLET -> R.string.send_error_address_same_as_wallet
                else -> null
            }
            return if (resId == null) null else context.getString(resId, "", "")
        }
        fg.imvPaste.isEnabled = state.pasteIsEnabled

        val et = fg.etAddressOrPayId
        val til = fg.tilAddressOrPayId
        val parsedError = parseError(til.context, state.error)

        til.isEnabled = state.inputIsEnabled
        fg.imvPaste.show(state.inputIsEnabled)
        fg.imvQrCode.show(state.inputIsEnabled)
        fg.flPaste.show(state.inputIsEnabled)
        fg.flQrCode.show(state.inputIsEnabled)

        val hintResId = if (state.sendingToPayIdEnabled) {
            R.string.send_destination_hint_address_payid
        } else {
            R.string.send_destination_hint_address
        }
        til.hint = til.getString(hintResId)
        til.error = parsedError
        til.isErrorEnabled = parsedError != null
        til.helperText = state.destinationWalletAddress
        til.isHelperTextEnabled = state.isPayIdState() && parsedError == null

        if (!state.viewFieldValue.isFromUserInput) et.update(state.viewFieldValue.value)
    }

    private fun handleAmountState(fg: BaseStoreFragment, state: AmountState) {
        if (state.error != null) {
            val context = fg.requireContext()
            val message = when (state.error) {
                is MultiMessageError -> {
                    val multiError = state.error as MultiMessageError
                    val messageList = multiError.assembleErrors().map { getMessageString(context, it.first, it.second) }
                    multiError.builder(messageList)
                }
                else -> context.getString(state.error.messageResource)
            }
            fg.tilAmountToSend.enableError(true, message)
        } else {
            fg.tilAmountToSend.enableError(false)
        }

        val filter = DecimalDigitsInputFilter(12, state.maxLengthOfAmount, state.decimalSeparator)
        fg.etAmountToSend.filters = arrayOf(filter)
        val amountToSend = state.viewAmountValue
        if (!amountToSend.isFromUserInput) fg.etAmountToSend.update(amountToSend.value)

        fg.tvAmountCurrency.update(state.mainCurrency.currencySymbol)
        (fg as? SendFragment)?.saveMainCurrency(state.mainCurrency.type)

        val balanceText = fg.getString(R.string.send_balance_subtitle_format,
            state.mainCurrency.currencySymbol,
            state.viewBalanceValue)
        fg.tvBalance.update(balanceText)

        fg.tilAmountToSend.isEnabled = state.inputIsEnabled

        val imageRes = if (state.inputIsEnabled) R.drawable.ic_arrows_up_down else 0
        fg.tvAmountCurrency.setCompoundDrawablesWithIntrinsicBounds(0, 0, imageRes, 0)
        val textColor = if (state.inputIsEnabled) R.color.blue else R.color.textGray
        fg.tvAmountCurrency.setTextColor(fg.getColor(textColor))

    }

    private fun handleFeeState(fg: BaseStoreFragment, state: FeeState) {
        fg.chipGroup.fitChipsByGroupWidth()
        fg.view?.findViewById<ViewGroup>(R.id.clNetworkFee)?.show(state.mainLayoutIsVisible)

        fg.imvExpandCollapse.rotation = if (state.controlsLayoutIsVisible) 0f else 180f
        fg.llFeeControlsContainer.show(state.controlsLayoutIsVisible)
        fg.chipGroup.show(state.feeChipGroupIsVisible)

        fg.swIncludeFee.isEnabled = state.includeFeeSwitcherIsEnabled
        if (fg.swIncludeFee.isChecked != state.feeIsIncluded) {
            fg.swIncludeFee.isChecked = state.feeIsIncluded
        }

        if (state.error == FeeAction.Error.REQUEST_FAILED) {
            fg.showRetrySnackbar(fg.requireContext().getString(R.string.send_error_fee_request_failed)) {
                store.dispatch(FeeAction.RequestFee)
            }
        }

        val chipId = FeeUiHelper.toId(state.selectedFeeType)
        if (fg.chipGroup.checkedChipId != chipId && chipId != View.NO_ID) fg.chipGroup.check(chipId)
    }

    private fun handleReceiptState(fg: BaseStoreFragment, state: ReceiptState) {
        val mainLayout = fg.clReceiptContainer as ViewGroup
        val totalLayout = fg.llTotal as ViewGroup
        val totalTokenLayout = fg.flTotalTokenCrypto as ViewGroup
        fun getString(id: Int, vararg formatStrings: String): String =
                mainLayout.context.getString(id, *formatStrings)

        val rough = getString(R.string.sign_rough)
        fun roughOrEmpty(value: String): String = if (value == ReceiptReducer.EMPTY) value else "$rough $value"

        when (state.visibleTypeOfReceipt) {
            ReceiptLayoutType.FIAT -> {
                val receipt = state.fiat ?: return

                totalLayout.show(true)
                totalTokenLayout.show(false)
                fg.tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                fg.tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                totalLayout.tvTotalValue.update("${roughOrEmpty(receipt.totalFiat)} ${receipt.symbols.fiat}")

                val willSent = getString(R.string.send_total_subtitle_format,
                    receipt.willSentCrypto, receipt.symbols.crypto)
                totalLayout.tvWillBeSentValue.update(willSent)

            }
            ReceiptLayoutType.CRYPTO -> {
                val receipt = state.crypto ?: return

                totalLayout.show(true)
                totalTokenLayout.show(false)
                fg.tvReceiptAmountValue.update("${receipt.amountCrypto} ${receipt.symbols.crypto}")
                fg.tvReceiptFeeValue.update("${receipt.feeCrypto} ${receipt.symbols.crypto}")
                totalLayout.tvTotalValue.update("${receipt.totalCrypto} ${receipt.symbols.crypto}")

                val willSent = SpannableStringBuilder()
                        .bold {
                            append(roughOrEmpty(receipt.willSentFiat)).append(" ")
                            append(receipt.symbols.fiat)
                            append(" (fee: ${receipt.feeFiat} ")
                            append(receipt.symbols.fiat).append(")")
                        }
                totalLayout.tvWillBeSentValue.update(willSent.toString())
            }
            ReceiptLayoutType.TOKEN_FIAT -> {
                val receipt = state.tokenFiat ?: return

                totalLayout.show(true)
                totalTokenLayout.show(false)
                fg.tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                fg.tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                totalLayout.tvTotalValue.update("${roughOrEmpty(receipt.totalFiat)} ${receipt.symbols.fiat}")

                val willSent = getString(
                    R.string.send_total_subtitle_asset_format,
                    receipt.symbols.token ?: "", receipt.willSentToken,
                    receipt.symbols.crypto, receipt.willSentFeeCoin
                )
                totalLayout.tvWillBeSentValue.update(willSent)
            }
            ReceiptLayoutType.TOKEN_CRYPTO -> {
                val receipt = state.tokenCrypto ?: return

                totalLayout.show(false)
                totalTokenLayout.show(true)

                fg.tvReceiptAmountValue.update("${receipt.amountToken} ${receipt.symbols.token}")
                fg.tvReceiptFeeValue.update("${receipt.feeCoin} ${receipt.symbols.crypto}")

                val willSent = SpannableStringBuilder()
                        .bold {
                            append(roughOrEmpty(receipt.totalFiat)).append(" ")
                            append(receipt.symbols.fiat)
                        }
                totalTokenLayout.tvTotalTokenCryptoValue.update(willSent.toString())
            }
        }
    }
}