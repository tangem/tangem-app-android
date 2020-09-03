package com.tangem.tap.features.send.redux

import android.view.View
import com.tangem.tap.features.send.redux.AddressPayIdActionUI.SetAddressOrPayId
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

private fun internalReduce(action: Action, sendState: SendState): SendState {
    if (action is ReleaseSendState) return SendState()
    val sendAction = action as? SendScreenAction ?: return sendState

    return when (sendAction) {
        is AddressPayIdActionUI -> handleAddressPayIdAction(sendAction, sendState, sendState.addressPayIDState)
        is FeeActionUI -> handleFeeLayoutAction(sendAction, sendState, sendState.feeLayoutState)
        else -> sendState
    }
}

private fun handleAddressPayIdAction(
        action: AddressPayIdActionUI,
        sendState: SendState,
        state: AddressPayIDState
): SendState {
    var state = state

    when (action) {
        is SetAddressOrPayId -> {
            state = state.copy(value = action.data?.toString())
            return updateLastState(sendState.copy(addressPayIDState = state), state)
        }
    }

    return sendState
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

private fun updateLastState(sendState: SendState, state: StateType): SendState {
    val sendState = sendState.copy(lastChangedStateType = state)
    Timber.d("$sendState")
    return sendState
}
