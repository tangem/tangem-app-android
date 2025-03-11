package com.tangem.features.send.impl.presentation.state.common

import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendSyncEditConverter(
    private val currentStateProvider: Provider<SendUiState>,
) : Converter<Boolean, SendUiState> {
    override fun convert(value: Boolean): SendUiState {
        val state = currentStateProvider()
        return if (value) {
            state.copy(
                amountState = state.editAmountState,
                feeState = state.editFeeState,
                recipientState = state.editRecipientState,
            )
        } else {
            state.copy(
                editAmountState = state.amountState,
                editRecipientState = state.recipientState,
                editFeeState = state.feeState,
            )
        }
    }
}