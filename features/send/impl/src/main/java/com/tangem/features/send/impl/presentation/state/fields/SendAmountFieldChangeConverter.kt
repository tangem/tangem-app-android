package com.tangem.features.send.impl.presentation.state.fields

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.text.DecimalFormatSymbols
import java.text.NumberFormat

internal class SendAmountFieldChangeConverter(
    private val currentStateProvider: Provider<SendUiState>,
) : Converter<String, SendUiState> {
    override fun convert(value: String): SendUiState {
        val state = currentStateProvider()
        val amountState = state.amountState ?: return state

        if (value.checkDecimalSeparatorDuplicate()) return state
        if (value.isEmpty()) return state.emptyState()

        val fiatRate = amountState.cryptoCurrencyStatus.value.fiatRate
        val trimmedValue = value.trim()
        val cryptoValue = if (amountState.isFiatValue) {
            if (value.isNotBlank()) {
                trimmedValue.toBigDecimal().divide(fiatRate)?.stripTrailingZeros()?.toPlainString().orEmpty()
            } else {
                DEFAULT_VALUE
            }
        } else {
            trimmedValue
        }

        val fiatValue = if (!amountState.isFiatValue) {
            if (value.isNotBlank()) {
                trimmedValue.toBigDecimal().multiply(fiatRate)?.stripTrailingZeros()?.toPlainString().orEmpty()
            } else {
                DEFAULT_VALUE
            }
        } else {
            trimmedValue
        }

        val isExceedBalance = value.checkExceedBalance(amountState.cryptoCurrencyStatus, amountState)
        return state.copy(
            amountState = amountState.copy(
                isPrimaryButtonEnabled = !isExceedBalance,
                amountTextField = amountState.amountTextField.copy(
                    value = cryptoValue,
                    fiatValue = fiatValue,
                    isError = isExceedBalance,
                ),
            ),
        )
    }

    private fun SendUiState.emptyState(): SendUiState {
        return copy(
            amountState = amountState?.copy(
                isPrimaryButtonEnabled = false,
                amountTextField = amountState.amountTextField.copy(
                    value = if (!amountState.isFiatValue) "" else DEFAULT_VALUE,
                    fiatValue = if (amountState.isFiatValue) "" else DEFAULT_VALUE,
                    isError = false,
                ),
            ),
        )
    }

    private fun String.checkDecimalSeparatorDuplicate(): Boolean {
        val regex = TRIM_REGEX.toRegex()
        val decimalSeparatorCount = regex.findAll(this).count()

        return decimalSeparatorCount > 1
    }

    private fun String.checkExceedBalance(
        cryptoCurrencyStatus: CryptoCurrencyStatus,
        state: SendStates.AmountState,
    ): Boolean {
        val currencyStatus = cryptoCurrencyStatus.value
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
        return trimmedValue.replace(TRIM_REGEX.toRegex(), separatorChar)
    }

    companion object {
        private val DEFAULT_VALUE = NumberFormat.getInstance().format(0.00)
        private const val TRIM_REGEX = "[.,]"
    }
}