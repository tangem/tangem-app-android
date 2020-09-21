package com.tangem.tap.features.send.redux.reducers

import com.tangem.tap.features.send.redux.AddressPayIdActionUi
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.AddressVerification
import com.tangem.tap.features.send.redux.AddressPayIdVerifyAction.PayIdVerification
import com.tangem.tap.features.send.redux.SendScreenAction
import com.tangem.tap.features.send.redux.states.AddressPayIdState
import com.tangem.tap.features.send.redux.states.InputViewValue
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
            is AddressPayIdActionUi.HandleUserInput -> state
            is AddressPayIdActionUi.SetTruncateHandler -> state.copy(truncateHandler = action.handler)
            is AddressPayIdActionUi.TruncateOrRestore -> {
                val value = if (action.truncate) state.truncatedFieldValue ?: ""
                else state.normalFieldValue ?: ""
                state.copy(viewFieldValue = state.viewFieldValue.copy(value = value))
            }
            is AddressPayIdActionUi.PasteAddressPayId -> return sendState
            is AddressPayIdActionUi.CheckClipboard -> return sendState
            is AddressPayIdActionUi.CheckAddressPayId -> return sendState
        }
        return updateLastState(sendState.copy(addressPayIdState = result), result)
    }

    private fun handleAction(action: AddressPayIdVerifyAction, sendState: SendState, state: AddressPayIdState): SendState {
        val result = when (action) {
            is PayIdVerification.SetPayIdWalletAddress -> {
                state.copy(
                        viewFieldValue = InputViewValue(action.payId, action.isUserInput),
                        normalFieldValue = action.payId,
                        truncatedFieldValue = state.truncate(action.payId),
                        recipientWalletAddress = action.payIdWalletAddress,
                        error = null
                )
            }
            is AddressVerification.SetWalletAddress -> {
                state.copy(
                        viewFieldValue = InputViewValue(action.address, action.isUserInput),
                        normalFieldValue = action.address,
                        truncatedFieldValue = state.truncate(action.address),
                        recipientWalletAddress = action.address,
                        error = null
                )
            }
            is AddressPayIdVerifyAction.ChangePasteBtnEnableState -> state.copy(pasteIsEnabled = action.isEnabled)
            is AddressVerification.SetAddressError -> state.copy(error = action.error, recipientWalletAddress = null)
            is PayIdVerification.SetPayIdError -> state.copy(error = action.error, recipientWalletAddress = null)
        }
        return updateLastState(sendState.copy(addressPayIdState = result), result)
    }
}