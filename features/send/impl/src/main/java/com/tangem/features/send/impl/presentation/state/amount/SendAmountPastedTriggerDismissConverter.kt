package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendAmountPastedTriggerDismissConverter(
    private val currentStateProvider: Provider<SendUiState>,
) : Converter<Boolean, SendUiState> {
    override fun convert(value: Boolean): SendUiState {
        val state = currentStateProvider()
        val amountState = state.amountState ?: return state
        return state.copy(
            amountState = amountState.copy(
                amountTextField = amountState.amountTextField.copy(
                    isValuePasted = false,
                ),
            ),
        )
    }
}