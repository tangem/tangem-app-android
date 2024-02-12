package com.tangem.features.send.impl.presentation.state.fields

import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendAmountFieldMaxAmountConverter(
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendUiState> {

    override fun convert(value: Unit): SendUiState {
        val state = currentStateProvider()
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val amountState = state.amountState ?: return state
        val amountTextField = amountState.amountTextField
        val feeState = state.feeState ?: return state

        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals
        val decimalCryptoValue = cryptoCurrencyStatus.value.amount
        val decimalFiatValue = cryptoCurrencyStatus.value.fiatAmount

        val cryptoValue = decimalCryptoValue?.parseBigDecimal(cryptoDecimals).orEmpty()
        val fiatValue = decimalFiatValue?.parseBigDecimal(fiatDecimals).orEmpty()
        return state.copy(
            amountState = amountState.copy(
                isPrimaryButtonEnabled = true,
                amountTextField = amountTextField.copy(
                    value = cryptoValue,
                    fiatValue = fiatValue,
                    isError = false,
                    cryptoAmount = amountTextField.cryptoAmount.copy(value = decimalCryptoValue),
                    fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                ),
            ),
            feeState = feeState.copy(
                isSubtract = true,
            ),
        )
    }
}