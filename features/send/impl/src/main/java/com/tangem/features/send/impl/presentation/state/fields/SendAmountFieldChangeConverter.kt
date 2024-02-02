package com.tangem.features.send.impl.presentation.state.fields

import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.math.RoundingMode

internal class SendAmountFieldChangeConverter(
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<String, SendUiState> {
    override fun convert(value: String): SendUiState {
        val state = currentStateProvider()
        val amountState = state.amountState ?: return state
        val amountTextField = amountState.amountTextField
        val feeState = state.feeState ?: return state

        if (value.isEmpty()) return state.emptyState()

        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val trimmedValue = value.trim()
        val cryptoValue = trimmedValue.getCryptoValue(amountTextField.isFiatValue, cryptoDecimals)
        val fiatValue = trimmedValue.getFiatValue(amountTextField.isFiatValue, fiatDecimals)
        val decimalCryptoValue = cryptoValue.parseToBigDecimal(cryptoDecimals)
        val decimalFiatValue = fiatValue.parseToBigDecimal(fiatDecimals)

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(amountTextField)
        val isMaxAmount = checkValue.checkMaxAmount(amountTextField)
        return state.copy(
            amountState = amountState.copy(
                isPrimaryButtonEnabled = !isExceedBalance,
                amountTextField = amountTextField.copy(
                    value = cryptoValue,
                    fiatValue = fiatValue,
                    isError = isExceedBalance,
                    cryptoAmount = amountTextField.cryptoAmount.copy(value = decimalCryptoValue),
                    fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                ),
            ),
            feeState = feeState.copy(
                isSubtract = isMaxAmount,
            ),
        )
    }

    private fun String.getCryptoValue(isFiatValue: Boolean, decimals: Int): String {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        return if (isFiatValue && fiatRate != null) {
            parseToBigDecimal(decimals).divide(fiatRate, decimals, RoundingMode.DOWN)
                .parseBigDecimal(decimals)
        } else {
            this
        }
    }

    private fun String.getFiatValue(isFiatValue: Boolean, decimals: Int): String {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        return if (!isFiatValue && fiatRate != null) {
            parseToBigDecimal(decimals).multiply(fiatRate).parseBigDecimal(decimals)
        } else {
            this
        }
    }

    private fun SendUiState.emptyState(): SendUiState {
        return copy(
            amountState = amountState?.copy(
                isPrimaryButtonEnabled = false,
                amountTextField = amountState.amountTextField.copy(
                    value = "",
                    fiatValue = "",
                    isError = false,
                ),
            ),
        )
    }

    private fun String.checkExceedBalance(amountTextField: SendTextField.AmountField): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val currencyCryptoAmount = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val currencyFiatAmount = cryptoCurrencyStatus.value.fiatAmount ?: BigDecimal.ZERO
        return if (amountTextField.isFiatValue) {
            parseToBigDecimal(amountTextField.fiatAmount.decimals) > currencyFiatAmount
        } else {
            parseToBigDecimal(amountTextField.cryptoAmount.decimals) > currencyCryptoAmount
        }
    }

    private fun String.checkMaxAmount(amountTextField: SendTextField.AmountField): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()

        // If current currency is Token
        if (cryptoCurrencyStatus.currency is CryptoCurrency.Token) return false

        val currencyCryptoAmount = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val currencyFiatAmount = cryptoCurrencyStatus.value.fiatAmount ?: BigDecimal.ZERO
        return if (amountTextField.isFiatValue) {
            parseToBigDecimal(amountTextField.fiatAmount.decimals) == currencyFiatAmount
        } else {
            parseToBigDecimal(amountTextField.cryptoAmount.decimals) == currencyCryptoAmount
        }
    }
}