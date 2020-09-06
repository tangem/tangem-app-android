package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.tap.features.send.redux.AddressPayIdActionUI.*
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification
import com.tangem.tap.features.send.redux.FeeActionUI.*
import org.rekotlin.Action
import org.rekotlin.StateType
import timber.log.Timber

/**
[REDACTED_AUTHOR]
 */
class SendReducer {
    companion object {
        fun reduce(action: Action, sendState: SendState): SendState {
            val newState = internalReduce(action, sendState)
            if (newState == sendState) Timber.i("state didn't modified.")
            else Timber.i("state was updated to: $newState.")
            return newState
        }
    }
}

private fun internalReduce(incomingAction: Action, sendState: SendState): SendState {
    if (incomingAction is ReleaseSendState) return SendState()
    val action = incomingAction as? SendScreenAction ?: return sendState

    return when (action) {
        is AddressPayIdActionUI -> handleAddressPayIdActionUI(action, sendState, sendState.addressPayIDState)
        is AddressPayIdVerifyAction -> handleAddressPayIdAction(action, sendState, sendState.addressPayIDState)
        is FeeActionUI -> handleFeeActionUI(action, sendState, sendState.feeLayoutState)
        else -> sendState
    }
}

fun handleAddressPayIdActionUI(
        action: AddressPayIdActionUI,
        sendState: SendState,
        state: AddressPayIDState
): SendState {
    val result = when (action) {
        is SetAddressOrPayId -> state
        is SetTruncateHandler -> state.copy(truncateHandler = action.handler)
        is TruncateOrRestore -> {
            if(action.truncate) state.copy(etFieldValue = state.truncatedFieldValue)
            else state.copy(etFieldValue = state.normalFieldValue)
        }
    }
    return updateLastState(sendState.copy(addressPayIDState = result), result)
}

private fun handleAddressPayIdAction(
        action: AddressPayIdVerifyAction,
        sendState: SendState,
        state: AddressPayIDState
): SendState {
    val result = when (action) {
        is PayIdVerification.Success -> state.copyPayIdWalletAddress(action.payId, action.payIdWalletAddress)
        is PayIdVerification.Failed -> state.copyPaiIdError(action.payId, action.reason)
        is AddressVerification.Success -> state.copyWalletAddress(action.address)
        is AddressVerification.Failed -> state.copyError(action.address, action.reason)
    }
    return updateLastState(sendState.copy(addressPayIDState = result), result)
}

private fun handleFeeActionUI(action: FeeActionUI, sendState: SendState, state: FeeLayoutState): SendState {
    val result = when (action) {
        is ToggleFeeLayoutVisibility -> {
            state.copy(visibility = if (state.visibility == View.VISIBLE) View.GONE else View.VISIBLE)
        }
        is ChangeSelectedFee -> state.copy(selectedFeeId = action.id)
        is ChangeIncludeFee -> state.copy(includeFeeIsChecked = action.isChecked)
    }
    return updateLastState(sendState.copy(feeLayoutState = result), result)
}

private fun updateLastState(sendState: SendState, lastChangedState: StateType): SendState =
        sendState.copy(lastChangedStateType = lastChangedState)
