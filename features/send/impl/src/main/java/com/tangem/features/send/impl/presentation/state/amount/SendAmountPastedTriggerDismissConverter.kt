package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendAmountPastedTriggerDismissConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
) : Converter<Boolean, SendUiState> {
    override fun convert(value: Boolean): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state
        return state.copyWrapped(
            isEditState = isEditState,
            amountState = amountState.copy(
                amountTextField = amountState.amountTextField.copy(
                    isValuePasted = false,
                ),
            ),
        )
    }
}
