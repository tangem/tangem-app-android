package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.common.ui.amountScreen.converters.AmountReduceToTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class SendAmountReduceToConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<BigDecimal, SendUiState> {

    override fun convert(value: BigDecimal): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state

        return state.copyWrapped(
            isEditState = isEditState,
            amountState = AmountReduceToTransformer(cryptoCurrencyStatusProvider(), value).transform(amountState),
        )
    }
}
