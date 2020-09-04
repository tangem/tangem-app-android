package com.tangem.tap.features.send.ui.stateSubscribers

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.tangem.tap.features.send.redux.AddressPayIDState
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.FailReason
import com.tangem.tap.features.send.redux.FeeLayoutState
import com.tangem.tap.features.send.redux.SendState
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.btn_expand_collapse.*
import kotlinx.android.synthetic.main.layout_send_address_payid.*
import kotlinx.android.synthetic.main.layout_send_network_fee.*

/**
* [REDACTED_AUTHOR]
 */
class SendStateSubscriber(fragment: Fragment) : FragmentStateSubscriber<SendState>(fragment) {

    override fun updateWithNewState(fg: Fragment, state: SendState) {
        when (state.lastChangedStateType) {
            is FeeLayoutState -> handleFeeLayoutState(fg, state.feeLayoutState)
            is AddressPayIDState -> handleAddressPayIdState(fg, state.addressPayIDState)
        }
    }

    private fun handleAddressPayIdState(fg: Fragment, state: AddressPayIDState) {
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

        val parsedError = parseError(fg.tilAddressOrPayId.context, state.error)
        fg.tilAddressOrPayId.error = parsedError
        fg.tilAddressOrPayId.isErrorEnabled = parsedError != null
        fg.tilAddressOrPayId.helperText = state.walletAddress
        fg.tilAddressOrPayId.isHelperTextEnabled = state.isPayIdState() && parsedError == null
        if (fg.etAddressOrPayId.text?.toString() != state.etFieldValue) {
            fg.etAddressOrPayId.setText(state.etFieldValue)
            if (fg.etAddressOrPayId.isFocused) fg.etAddressOrPayId.setSelection(state.etFieldValue?.length ?: 0)
        }
    }

    private fun handleFeeLayoutState(fg: Fragment, layoutState: FeeLayoutState) {
        if (fg.llFeeContainer.visibility != layoutState.visibility) {
            val rotationAngle = if (fg.imvExpandCollapse.rotation == 0f) 180f else 0f
            fg.imvExpandCollapse.rotation = rotationAngle

            (fg.llFeeContainer.parent?.parent as? ViewGroup)?.let { TransitionManager.beginDelayedTransition(it) }
            fg.llFeeContainer.visibility = layoutState.visibility
        }

        if (fg.swIncludeFee.isChecked != layoutState.includeFeeIsChecked) {
            fg.swIncludeFee.isChecked = layoutState.includeFeeIsChecked
        }

        if (fg.chipGroup.checkedChipId != layoutState.selectedFeeId) {
            fg.chipGroup.check(layoutState.selectedFeeId)
        }
    }
}