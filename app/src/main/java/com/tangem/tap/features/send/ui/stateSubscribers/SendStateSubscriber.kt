package com.tangem.tap.features.send.ui.stateSubscribers

import android.content.Context
import android.util.TypedValue
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tangem.tap.common.extensions.beginDelayedTransition
import com.tangem.tap.common.extensions.enableError
import com.tangem.tap.common.extensions.update
import com.tangem.tap.features.send.redux.AddressPayIdState
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.FailReason
import com.tangem.tap.features.send.redux.AmountState
import com.tangem.tap.features.send.redux.FeeLayoutState
import com.tangem.tap.features.send.redux.SendState
import com.tangem.tap.features.send.ui.FeeUiHelper
import com.tangem.tap.features.send.ui.SendFragment
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.btn_expand_collapse.*
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_amount.*
import kotlinx.android.synthetic.main.layout_send_network_fee.*

/**
[REDACTED_AUTHOR]
 */
class SendStateSubscriber(fragment: Fragment) : FragmentStateSubscriber<SendState>(fragment) {

    override fun updateWithNewState(fg: Fragment, state: SendState) {
        when (state.lastChangedStateType) {
            is FeeLayoutState -> handleFeeLayoutState(fg, state.feeLayoutState)
            is AddressPayIdState -> handleAddressPayIdState(fg, state.addressPayIdState)
            is AmountState -> handleAmountState(fg, state.amountState)
        }

        fg.btnSend.isEnabled = state.sendButtonIsEnabled
    }

    private fun handleAddressPayIdState(fg: Fragment, state: AddressPayIdState) {
        fun parseError(context: Context, error: FailReason?): String? {
            val resId = when (error) {
                FailReason.IS_NOT_PAY_ID -> R.string.error_payid_verification_failed
                FailReason.PAY_ID_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.error_payid_unsupported_by_blockchain
                FailReason.PAY_ID_NOT_REGISTERED -> R.string.error_payid_not_registere
                FailReason.PAY_ID_REQUEST_FAILED -> R.string.error_payid_request_failed
                FailReason.ADDRESS_INVALID_OR_UNSUPPORTED_BY_BLOCKCHAIN -> R.string.error_address_invalid_or_unsupported
                FailReason.ADDRESS_SAME_AS_WALLET -> R.string.error_address_same_as_wallet
                else -> null
            }
            return if (resId == null) null
            else context.getString(resId)
        }

        val et = fg.etAddressOrPayId
        val til = fg.tilAddressOrPayId
        val parsedError = parseError(til.context, state.error)

        til.error = parsedError
        til.isErrorEnabled = parsedError != null
        til.helperText = state.walletAddress
        til.isHelperTextEnabled = state.isPayIdState() && parsedError == null

        // prevent cycling
        if (state.etFieldValue == null) return

        et.update(state.etFieldValue)
    }

    private fun handleFeeLayoutState(fg: Fragment, layoutState: FeeLayoutState) {
        if (fg.llFeeContainer.visibility != layoutState.visibility) {
            val rotationAngle = if (fg.imvExpandCollapse.rotation == 0f) 180f else 0f
            fg.imvExpandCollapse.rotation = rotationAngle

            (fg.llFeeContainer.parent?.parent as? ViewGroup)?.beginDelayedTransition()
            fg.llFeeContainer.visibility = layoutState.visibility
        }

        if (fg.swIncludeFee.isChecked != layoutState.feeIsIncluded) {
            fg.swIncludeFee.isChecked = layoutState.feeIsIncluded
        }

        val chipId = FeeUiHelper.feeToId(layoutState.feeType)
        if (fg.chipGroup.checkedChipId != chipId) fg.chipGroup.check(chipId)
    }

    private fun handleAmountState(fg: Fragment, state: AmountState) {
        fg.tilAmountToSend.enableError(state.amountIsOverBalance)

        val amountToSend = state.etAmountFieldValue
        fg.tvAmountToSendShadow.text = amountToSend
        if (amountToSend.length > 10) {
            // post is needed to wait for text size changes
            fg.tvAmountToSendShadow.post {
                fg.etAmountToSend.setTextSize(TypedValue.COMPLEX_UNIT_PX, fg.tvAmountToSendShadow.textSize - 2)
                fg.etAmountToSend.update(amountToSend)
                if (!state.cursorAtTheSamePosition) fg.etAmountToSend.setSelection(amountToSend.length)
            }
        } else {
            val textSize = fg.resources.getDimension(R.dimen.text_size_amount_to_send)
            fg.tvAmountToSendShadow.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            fg.etAmountToSend.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            fg.etAmountToSend.update(amountToSend)
            if (!state.cursorAtTheSamePosition) fg.etAmountToSend.setSelection(amountToSend.length)
        }

        fg.tvAmountCurrency.update(state.mainCurrency.displayedValue)
        (fg as? SendFragment)?.let { it.saveMainCurrency(state.mainCurrency.value) }

        val balanceText = fg.getString(R.string.send_balance,
                state.mainCurrency.displayedValue,
                state.balance.toPlainString())
        fg.tvBalance.update(balanceText)
    }
}