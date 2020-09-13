package com.tangem.tap.features.send.redux.reducers

import com.tangem.tap.features.send.redux.AddressPayIdActionUi
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AddressPayIdState
import com.tangem.tap.features.send.redux.states.SendState

/**
[REDACTED_AUTHOR]
 */
class AddressPayIdReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = when (action) {
        is AddressPayIdActionUi -> handleUiAction(action, sendState, sendState.addressPayIdState)
        is AddressPayIdVerifyAction -> handleAction(action, sendState, sendState.addressPayIdState)
        else -> sendState
    }

    private fun handleUiAction(action: AddressPayIdActionUi, sendState: SendState, state: AddressPayIdState): SendState {
        val result = when (action) {
            is AddressPayIdActionUi.ChangeAddressOrPayId -> state
            is AddressPayIdActionUi.SetTruncateHandler -> state.copy(truncateHandler = action.handler)
            is AddressPayIdActionUi.TruncateOrRestore -> {
                if (action.truncate) state.copy(etFieldValue = state.truncatedFieldValue)
                else state.copy(etFieldValue = state.normalFieldValue)
            }
        }
        return updateLastState(sendState.copy(addressPayIdState = result), result)
    }

    private fun handleAction(action: AddressPayIdVerifyAction, sendState: SendState, state: AddressPayIdState): SendState {
        val result = when (action) {
            is PayIdVerification.SetPayIdWalletAddress -> state.copyPayIdWalletAddress(action.payId, action.payIdWalletAddress)
            is PayIdVerification.SetError -> state.copyPayIdError(action.payId, action.error)
            is AddressVerification.SetWalletAddress -> state.copyWalletAddress(action.address)
            is AddressVerification.SetError -> state.copyError(action.address, action.error)
        }
        return updateLastState(sendState.copy(addressPayIdState = result), result)
    }
}