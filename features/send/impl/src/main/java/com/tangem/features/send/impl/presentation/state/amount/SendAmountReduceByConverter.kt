package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceByTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendAmountReduceByConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<AmountReduceByTransformer.ReduceByData, SendUiState> {

    override fun convert(value: AmountReduceByTransformer.ReduceByData): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state

        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(
                reduceAmountBy = value.reduceAmountBy,
            ),
            amountState = AmountReduceByTransformer(cryptoCurrencyStatusProvider(), value).transform(amountState),
        )
    }
}
