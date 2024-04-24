package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldChangeConverter
import com.tangem.features.send.impl.presentation.state.fields.SendAmountFieldMaxAmountConverter
import com.tangem.utils.Provider

/**
 * Factory to produce amount state for [SendUiState]
 */
internal class AmountStateFactory(
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) {

    private val amountFieldChangeConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountFieldChangeConverter(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val amountFieldMaxAmountConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountFieldMaxAmountConverter(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    private val amountCurrencyConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountCurrencyConverter(
            currentStateProvider = currentStateProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }
    private val amountPasteConverter by lazy(LazyThreadSafetyMode.NONE) {
        SendAmountPastedTriggerDismissConverter(
            currentStateProvider = currentStateProvider,
        )
    }

    fun getOnAmountValueChange(value: String) = amountFieldChangeConverter.convert(value)

    fun getOnMaxAmountClick(): SendUiState {
        return amountFieldMaxAmountConverter.convert(Unit)
    }

    fun getOnCurrencyChangedState(isFiat: Boolean) = amountCurrencyConverter.convert(isFiat)

    fun getOnAmountPastedTriggerDismiss() = amountPasteConverter.convert(false)
}
