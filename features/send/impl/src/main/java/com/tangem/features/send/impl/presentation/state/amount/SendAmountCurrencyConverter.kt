package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.common.ui.amountScreen.converters.AmountCurrencyTransformer
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendAmountCurrencyConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Boolean, SendUiState> {

    override fun convert(value: Boolean): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) as? AmountState.Data ?: return state

        return state.copyWrapped(
            isEditState = isEditState,
            amountState = AmountCurrencyTransformer(cryptoCurrencyStatusProvider(), value).transform(amountState),
        )
    }
}
