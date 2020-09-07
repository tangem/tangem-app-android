package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.tap.features.send.redux.AddressPayIdActionUi.*
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification
import com.tangem.tap.features.send.redux.FeeActionUi.*
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
        is AddressPayIdActionUi -> handleAddressPayIdActionUi(action, sendState, sendState.addressPayIdState)
        is AddressPayIdVerifyAction -> handleAddressPayIdAction(action, sendState, sendState.addressPayIdState)
        is FeeActionUi -> handleFeeActionUi(action, sendState, sendState.feeLayoutState)
        else -> sendState
    }
}

fun handleAddressPayIdActionUi(
        action: AddressPayIdActionUi,
        sendState: SendState,
        state: AddressPayIdState
): SendState {
    val result = when (action) {
        is SetAddressOrPayId -> state
        is SetTruncateHandler -> state.copy(truncateHandler = action.handler)
        is TruncateOrRestore -> {
            if(action.truncate) state.copy(etFieldValue = state.truncatedFieldValue)
            else state.copy(etFieldValue = state.normalFieldValue)
        }
    }
    return updateLastState(sendState.copy(addressPayIdState = result), result)
}

private fun handleAddressPayIdAction(
        action: AddressPayIdVerifyAction,
        sendState: SendState,
        state: AddressPayIdState
): SendState {
    val result = when (action) {
        is PayIdVerification.SetPayIdWalletAddress -> state.copyPayIdWalletAddress(action.payId, action.payIdWalletAddress)
        is PayIdVerification.SetError -> state.copyPaiIdError(action.payId, action.reason)
        is AddressVerification.SetWalletAddress -> state.copyWalletAddress(action.address)
        is AddressVerification.SetError -> state.copyError(action.address, action.reason)
    }
    return updateLastState(sendState.copy(addressPayIdState = result), result)
}

private fun handleFeeActionUi(action: FeeActionUi, sendState: SendState, state: FeeLayoutState): SendState {
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
