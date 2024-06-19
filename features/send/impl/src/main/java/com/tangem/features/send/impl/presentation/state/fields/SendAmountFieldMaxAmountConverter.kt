package com.tangem.features.send.impl.presentation.state.fields

import com.tangem.common.ui.amountScreen.converters.field.AmountFieldMaxAmountTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero

internal class SendAmountFieldMaxAmountConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendUiState> {

    override fun convert(value: Unit): SendUiState {
        val state = currentStateProvider()
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state

        val decimalCryptoValue = cryptoCurrencyStatus.value.amount
        if (decimalCryptoValue.isNullOrZero()) return state

        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(reduceAmountBy = null),
            amountState = AmountFieldMaxAmountTransformer(cryptoCurrencyStatusProvider()).transform(amountState),
        )
    }
}
