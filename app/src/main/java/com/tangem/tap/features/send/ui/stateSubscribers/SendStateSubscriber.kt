package com.tangem.tap.features.send.ui.stateSubscribers

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.ViewGroup
import androidx.core.text.bold
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.enableError
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.extensions.update
import com.tangem.tap.common.text.DecimalDigitsInputFilter
import com.tangem.tap.common.toggleWidget.ProgressState
import com.tangem.tap.domain.MultiMessageError
import com.tangem.tap.domain.assembleErrorIds
import com.tangem.tap.features.send.BaseStoreFragment
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.Error
import com.tangem.tap.features.send.redux.FeeAction
import com.tangem.tap.features.send.redux.states.*
import com.tangem.tap.features.send.ui.FeeUiHelper
import com.tangem.tap.features.send.ui.SendFragment
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
* [REDACTED_AUTHOR]
 */
class SendStateSubscriber(fragment: BaseStoreFragment) : FragmentStateSubscriber<SendState>(fragment) {

    override fun updateWithNewState(fg: BaseStoreFragment, state: SendState) {
        val lastChangedStates = state.lastChangedStates.toList()
        state.lastChangedStates.clear()
        lastChangedStates.forEach {
            when (it) {
                StateId.ADDRESS_PAY_ID -> handleAddressPayIdState(fg, state.addressPayIdState)
                StateId.AMOUNT -> handleAmountState(fg, state.amountState)
                StateId.FEE -> handleFeeState(fg, state.feeState)
                StateId.RECEIPT -> handleReceiptState(fg, state.receiptState)
            }
        }

        val sendFragment = (fg as? SendFragment) ?: return
        when (state.sendButtonState) {
            SendButtonState.ENABLED -> {
                fg.btnSend.isEnabled = true
                sendFragment.sendBtn.setState(ProgressState.None(), true)
            }
            SendButtonState.DISABLED -> {
                fg.btnSend.isEnabled = false
                sendFragment.sendBtn.setState(ProgressState.None(), true)
            }
            SendButtonState.PROGRESS -> {
                fg.btnSend.isEnabled = true
                sendFragment.sendBtn.setState(ProgressState.Progress(), true)
            }
        }
    }

    private fun handleAddressPayIdState(fg: BaseStoreFragment, state: AddressPayIdState) {
        fun parseError(context: Context, error: Error?): String? {
            val resId = when (error) {
                Error.IS_NOT_PAY_ID -> R.string.error_payid_verification_failed
                Error.PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.error_payid_unsupported_by_blockchain
                Error.PAY_ID_NOT_REGISTERED -> R.string.error_payid_not_registere
                Error.PAY_ID_REQUEST_FAILED -> R.string.error_payid_request_failed
                Error.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.error_address_invalid_or_unsupported
                Error.ADDRESS_SAME_AS_WALLET -> R.string.error_address_same_as_wallet
                else -> null
            }
            return if (resId == null) null
            else context.getString(resId)
        }
        fg.imvPaste.isEnabled = state.pasteIsEnabled

        val et = fg.etAddressOrPayId
        val til = fg.tilAddressOrPayId
        val parsedError = parseError(til.context, state.error)

        til.parent?.parent?.beginDelayedTransition()
        til.error = parsedError
        til.isErrorEnabled = parsedError != null
        til.helperText = state.recipientWalletAddress
        til.isHelperTextEnabled = state.isPayIdState() && parsedError == null

        // prevent cycling
        if (state.etFieldValue == null) return

        et.update(state.etFieldValue)
    }

    private fun handleAmountState(fg: BaseStoreFragment, state: AmountState) {
        if (state.error != null) {
            val context = fg.requireContext()
            val message = when (state.error) {
                is MultiMessageError -> {
                    val multiError = state.error as MultiMessageError
                    val messageList = multiError.assembleErrorIds().map { context.getString(it) }
                    multiError.builder(messageList)
                }
                else -> context.getString(state.error.localizedMessage)
            }
            fg.amountContainer.parent?.beginDelayedTransition()
            fg.tilAmountToSend.enableError(true, message)
        } else {
            if (fg.tilAmountToSend.isErrorEnabled) fg.amountContainer.parent?.beginDelayedTransition()
            fg.tilAmountToSend.enableError(false)
        }

        fg.etAmountToSend.filters = arrayOf(DecimalDigitsInputFilter(12, state.maxLengthOfAmount))
        val amountToSend = state.viewAmountValue
        if (!amountToSend.isUserInput) fg.etAmountToSend.update(amountToSend.value)

//        fg.tvAmountToSendShadow.text = amountToSend
//        if (amountToSend.length > 10) {
//            post is needed to wait for text size changes
//            fg.tvAmountToSendShadow.post {
//                fg.etAmountToSend.setTextSize(TypedValue.COMPLEX_UNIT_PX, fg.tvAmountToSendShadow.textSize - 2)
//                fg.etAmountToSend.update(amountToSend)
//                if (!state.cursorAtTheSamePosition) fg.etAmountToSend.setSelection(amountToSend.length)
//            }
//        } else {
//            val textSize = fg.resources.getDimension(R.dimen.text_size_amount_to_send)
//            fg.tvAmountToSendShadow.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
//            fg.etAmountToSend.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
//            fg.etAmountToSend.update(amountToSend)
//            if (!state.cursorAtTheSamePosition) fg.etAmountToSend.setSelection(amountToSend.length)
//        }

        fg.tvAmountCurrency.update(state.mainCurrency.currencySymbol)
        (fg as? SendFragment)?.saveMainCurrency(state.mainCurrency.type)

        val balanceText = fg.getString(R.string.send_balance,
                state.mainCurrency.currencySymbol,
                state.viewBalanceValue)
        fg.tvBalance.update(balanceText)
    }

    private fun handleFeeState(fg: BaseStoreFragment, state: FeeState) {
        var delayedTransitionScheduled = false
        fg.view?.findViewById<ViewGroup>(R.id.clNetworkFee)?.let {
            it.show(state.mainLayoutIsVisible) {
                (it.parent as? ViewGroup)?.beginDelayedTransition()
                delayedTransitionScheduled = true
            }
        }

        fg.imvExpandCollapse.rotation = if (state.controlsLayoutIsVisible) 0f else 180f
        fg.llFeeControlsContainer.show(state.controlsLayoutIsVisible) {
            if (!delayedTransitionScheduled) {
                fg.llFeeControlsContainer.parent?.parent?.beginDelayedTransition()
            }
        }

        fg.chipGroup.show(state.feeChipGroupIsVisible) {
            if (!delayedTransitionScheduled) {
                fg.llFeeControlsContainer.parent?.parent?.beginDelayedTransition()
            }
        }

        fg.swIncludeFee.isEnabled = state.includeFeeSwitcherIsEnabled
        if (fg.swIncludeFee.isChecked != state.feeIsIncluded) {
            fg.swIncludeFee.isChecked = state.feeIsIncluded
        }

        if (state.error == FeeAction.Error.REQUEST_FAILED) {
            fg.showRetrySnackbar(fg.requireContext().getString(R.string.error_fee_request_failed)) {
                store.dispatch(FeeAction.RequestFee)
            }
        }

        val chipId = FeeUiHelper.feeToId(state.selectedFeeType)
        if (fg.chipGroup.checkedChipId != chipId && chipId != 0) fg.chipGroup.check(chipId)
    }

    private fun handleReceiptState(fg: BaseStoreFragment, state: ReceiptState) {
        val mainLayout = fg.clReceiptContainer as ViewGroup
        val totalLayout = fg.llTotal as ViewGroup
        val totalTokenLayout = fg.flTotalTokenCrypto as ViewGroup
        fun getString(id: Int): String = mainLayout.context.getString(id)

        val rough = getString(R.string.sign_rough)
        when (state.visibleTypeOfReceipt) {
            ReceiptLayoutType.FIAT -> {
                val receipt = state.fiat ?: return

                totalLayout.show(true)
                totalTokenLayout.show(false)
                fg.tvReceiptAmountValue.update("${receipt.amountFiat} ${receipt.symbols.fiat}")
                fg.tvReceiptFeeValue.update("${receipt.feeFiat} ${receipt.symbols.fiat}")
                totalLayout.tvTotalValue.update("$rough ${receipt.totalFiat} ${receipt.symbols.fiat}")

                val willSent = SpannableStringBuilder()
                        .bold { append(receipt.willSentCrypto) }.append(" ")
                        .append(receipt.symbols.crypto).append(" ")
                        .append(getString(R.string.send_total_will_be_sent))
                totalLayout.tvWillBeSentValue.update(willSent.toString())

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
                            append(rough).append(" ")
                            append(receipt.willSentFiat).append(" ")
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
                totalLayout.tvTotalValue.update("${receipt.totalFiat} ${receipt.symbols.fiat}")

                val willSent = SpannableStringBuilder()
                        .bold {
                            append(receipt.symbols.token)
                            append(" ")
                            append(receipt.willSentToken)
                        }.append(" ").append(getString(R.string.generic_and)).append(" ")
                        .bold {
                            append(receipt.symbols.crypto).append(" ")
                            append(receipt.willSentFeeCoin).append(" ")
                        }
                        .append(mainLayout.context.getString(R.string.send_total_will_be_sent))
                totalLayout.tvWillBeSentValue.update(willSent.toString())
            }
            ReceiptLayoutType.TOKEN_CRYPTO -> {
                val receipt = state.tokenCrypto ?: return

                totalLayout.show(false)
                totalTokenLayout.show(true)

                fg.tvReceiptAmountValue.update("${receipt.amountToken} ${receipt.symbols.token}")
                fg.tvReceiptFeeValue.update("${receipt.feeCoin} ${receipt.symbols.crypto}")

                val willSent = SpannableStringBuilder()
                        .bold {
                            append(getString(R.string.sign_rough)).append(" ")
                            append(receipt.totalFiat).append(" ")
                            append(receipt.symbols.fiat)
                        }
                totalTokenLayout.tvTotalTokenCryptoValue.update(willSent.toString())
            }
        }
    }
}