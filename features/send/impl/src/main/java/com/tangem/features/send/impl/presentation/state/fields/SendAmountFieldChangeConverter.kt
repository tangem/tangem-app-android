package com.tangem.features.send.impl.presentation.state.fields

import com.tangem.common.Provider
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.converter.Converter
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

internal class SendAmountFieldChangeConverter(
    private val currentStateProvider: Provider<SendUiState>,
) : Converter<String, SendUiState> {
    override fun convert(value: String): SendUiState {
        val state = currentStateProvider()

        if (
            state !is SendUiState.Content.AmountState ||
            value.checkDecimalSeparatorDuplicate()
        ) {
            return state
        }

        if (value.isEmpty()) return state.emptyState()

        val fiatRate = state.cryptoCurrencyStatus.value.fiatRate

        val trimmedValue = value.trim()

        val cryptoValue = if (state.isFiatValue) {
            if (value.isNotBlank()) {
                trimmedValue.toBigDecimal().divide(fiatRate)?.stripTrailingZeros()?.toPlainString().orEmpty()
            } else {
                DEFAULT_VALUE
            }
        } else {
            trimmedValue
        }

        val fiatValue = if (!state.isFiatValue) {
            if (value.isNotBlank()) {
                trimmedValue.toBigDecimal().multiply(fiatRate)?.stripTrailingZeros()?.toPlainString().orEmpty()
            } else {
                DEFAULT_VALUE
            }
        } else {
            trimmedValue
        }

        val isExceedBalance = value.checkExceedBalance(state)
        return state.copy(
            amountTextField = state.amountTextField.copy(
                value = cryptoValue,
                fiatValue = fiatValue,
                isError = isExceedBalance,
            ),
            isPrimaryButtonEnabled = !isExceedBalance,
        )
    }

    private fun SendUiState.Content.AmountState.emptyState(): SendUiState {
        return copy(
            amountTextField = amountTextField.copy(
                value = if (!isFiatValue) "" else DEFAULT_VALUE,
                fiatValue = if (isFiatValue) "" else DEFAULT_VALUE,
                isError = false,
            ),
            isPrimaryButtonEnabled = false,
        )
    }

    private fun String.checkDecimalSeparatorDuplicate(): Boolean {
        val regex = "[\\.\\,]".toRegex()
        val decimalSeparatorCount = regex.findAll(this).count()

        return decimalSeparatorCount > 1
    }

    private fun String.checkExceedBalance(state: SendUiState.Content.AmountState): Boolean {
        val currencyStatus = state.cryptoCurrencyStatus.value
        return if (state.isFiatValue) {
            toBigDecimal() > currencyStatus.fiatAmount
        } else {
            toBigDecimal() > currencyStatus.amount
        }
    }

    private fun String.trim(): String {
        var trimmedValue = this
        if (length > 1 && firstOrNull() == '0' && get(1).isDigit()) trimmedValue = drop(1)

        val separatorChar = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
        return trimmedValue.replace("[\\.\\,]".toRegex(), separatorChar)
    }

    companion object {
        private val DEFAULT_VALUE = NumberFormat.getInstance().format(0.00)
    }
}