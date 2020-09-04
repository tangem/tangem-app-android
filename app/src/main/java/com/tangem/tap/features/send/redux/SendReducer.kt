package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.tap.features.send.redux.FeeActionUI.*
import org.rekotlin.Action
import org.rekotlin.StateType
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class SendReducer {
    companion object {
        fun reduce(action: Action, sendState: SendState): SendState = internalReduce(action, sendState)
    }
}

private fun internalReduce(incomingAction: Action, sendState: SendState): SendState {
    if (incomingAction is ReleaseSendState) return SendState()
    val action = incomingAction as? SendScreenAction ?: return sendState

    return when (action) {
        is FeeActionUI -> handleFeeLayoutAction(action, sendState, sendState.feeLayoutState)
//        is SetAddressOrPayId -> handleAddressPayIdUIAction(action, sendState, sendState.addressPayIDState)
        is AddressPayIdVerifyAction -> handleAddressPayIdAction(action, sendState, sendState.addressPayIDState)
        else -> sendState
    }
}

private fun handleAddressPayIdAction(
        action: AddressPayIdVerifyAction,
        sendState: SendState,
        state: AddressPayIDState
): SendState {
    val result: AddressPayIDState? = when (action) {
        is AddressPayIdVerifyAction.PayIdVerification.Success -> {
            state.copyPayIdWalletAddress(action.payId, action.payIdWalletAddress)
        }
        is AddressPayIdVerifyAction.PayIdVerification.Failed -> {
            state.copyPaiIdError(action.payId, action.reason)
        }
        is AddressPayIdVerifyAction.AddressVerification.Success -> {
            state.copyWalletAddress(action.address)
        }
        is AddressPayIdVerifyAction.AddressVerification.Failed -> {
            state.copyError(action.address, action.reason)
        }
        is AddressPayIdVerifyAction.ProcessedButNotVerifiedAddressPayId -> {
            state.copy(etFieldValue = action.data)
        }
    }
    return if (result == null) {
        Timber.e("AddressPayIDState didn't modified.")
        sendState
    } else {
        updateLastState(sendState.copy(addressPayIDState = result), result)
    }

}

private fun handleFeeLayoutAction(action: FeeActionUI, sendState: SendState, state: FeeLayoutState): SendState {
    return when (action) {
        is ToggleFeeLayoutVisibility -> {
            val visibility = if (state.visibility == View.VISIBLE) View.GONE
            else View.VISIBLE

            val result = state.copy(visibility = visibility)
            updateLastState(sendState.copy(feeLayoutState = result), result)
        }
        is ChangeSelectedFee -> {
            val result = state.copy(selectedFeeId = action.id)
            updateLastState(sendState.copy(feeLayoutState = result), result)
        }
        is ChangeIncludeFee -> {
            val result = state.copy(includeFeeIsChecked = action.isChecked)
            updateLastState(sendState.copy(feeLayoutState = result), result)
        }
    }
}

private fun updateLastState(sendState: SendState, lastChangedState: StateType): SendState {
    val sendState = sendState.copy(lastChangedStateType = lastChangedState)
    Timber.d("$sendState")
    return sendState
}
