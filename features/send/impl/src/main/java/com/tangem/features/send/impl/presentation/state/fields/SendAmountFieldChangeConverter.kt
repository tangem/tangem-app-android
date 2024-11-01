package com.tangem.features.send.impl.presentation.state.fields

import com.tangem.common.ui.amountScreen.converters.MaxEnterAmountConverter
import com.tangem.common.ui.amountScreen.converters.field.AmountFieldChangeTransformer
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendAmountFieldChangeConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<String, SendUiState> {

    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    override fun convert(value: String): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState)

        val maxEnterAmount = maxEnterAmountConverter.convert(cryptoCurrencyStatusProvider())

        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(reduceAmountBy = null),
            amountState = AmountFieldChangeTransformer(maxEnterAmount, value).transform(amountState),
        )
    }
}
