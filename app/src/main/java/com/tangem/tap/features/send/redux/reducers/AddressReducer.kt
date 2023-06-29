package com.tangem.tap.features.send.redux.reducers

import com.tangem.tap.features.send.redux.AddressActionUi
import com.tangem.tap.features.send.redux.AddressVerifyAction
import com.tangem.tap.features.send.redux.AddressVerifyAction.AddressVerification
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AddressState
import com.tangem.tap.features.send.redux.states.InputViewValue
import com.tangem.tap.features.send.redux.states.SendState

/**
[REDACTED_AUTHOR]
 */
class AddressReducer : SendInternalReducer {
    override fun handle(action: SendScreenAction, sendState: SendState): SendState = when (action) {
        is AddressActionUi -> handleUiAction(action, sendState, sendState.addressState)
        is AddressVerifyAction -> handleAction(action, sendState, sendState.addressState)
        else -> sendState
    }

    private fun handleUiAction(action: AddressActionUi, sendState: SendState, state: AddressState): SendState {
        val result = when (action) {
            is AddressActionUi.HandleUserInput -> state
            is AddressActionUi.SetTruncateHandler -> state.copy(truncateHandler = action.handler)
            is AddressActionUi.TruncateOrRestore -> {
                val value = if (action.truncate) state.truncatedFieldValue ?: "" else state.normalFieldValue ?: ""
                state.copy(viewFieldValue = state.viewFieldValue.copy(value = value))
            }
            is AddressActionUi.PasteAddress -> return sendState
            is AddressActionUi.CheckClipboard -> return sendState
            is AddressActionUi.CheckAddress -> return sendState
        }
        return updateLastState(sendState.copy(addressState = result), result)
    }

    private fun handleAction(action: AddressVerifyAction, sendState: SendState, state: AddressState): SendState {
        val result = when (action) {
            is AddressVerification.SetWalletAddress -> {
                state.copy(
                    viewFieldValue = InputViewValue(action.address, action.isUserInput),
                    normalFieldValue = action.address,
                    truncatedFieldValue = state.truncate(action.address),
                    destinationWalletAddress = action.address,
                    error = null,
                )
            }
            is AddressVerifyAction.ChangePasteBtnEnableState -> state.copy(pasteIsEnabled = action.isEnabled)
            is AddressVerification.SetAddressError -> state.copy(error = action.error, destinationWalletAddress = null)
        }
        return updateLastState(sendState.copy(addressState = result), result)
    }
}