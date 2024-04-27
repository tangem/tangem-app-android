package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal
import java.math.RoundingMode

internal class SendAmountFieldChangeConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<String, SendUiState> {
    override fun convert(value: String): SendUiState {
        val state = currentStateProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state
        val amountTextField = amountState.amountTextField

        if (value.isEmpty()) return state.emptyState()
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val trimmedValue = value.trim()
        val cryptoValue = trimmedValue.getCryptoValue(amountTextField.isFiatValue, cryptoDecimals)
        val decimalCryptoValue = cryptoValue.parseToBigDecimal(cryptoDecimals)
        val (fiatValue, decimalFiatValue) = trimmedValue.getFiatValue(
            isFiatValue = amountTextField.isFiatValue,
            decimals = fiatDecimals,
        )

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(amountTextField)
        val isZero = if (amountTextField.isFiatValue) decimalFiatValue.isNullOrZero() else decimalCryptoValue.isZero()
        return state.copyWrapped(
            isEditState = isEditState,
            amountState = amountState.copy(
                isPrimaryButtonEnabled = !isExceedBalance && !isZero,
                amountTextField = amountTextField.copy(
                    value = cryptoValue,
                    fiatValue = fiatValue,
                    isError = isExceedBalance,
                    cryptoAmount = amountTextField.cryptoAmount.copy(value = decimalCryptoValue),
                    fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                    keyboardOptions = KeyboardOptions(
                        imeAction = getKeyboardAction(isExceedBalance, decimalCryptoValue),
                        keyboardType = KeyboardType.Number,
                    ),
                ),
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

    private fun String.getFiatValue(isFiatValue: Boolean, decimals: Int): Pair<String, BigDecimal?> {
        val fiatRate = cryptoCurrencyStatusProvider().value.fiatRate
        return if (fiatRate != null) {
            val fiatValue = if (!isFiatValue) {
                parseToBigDecimal(decimals).multiply(fiatRate).parseBigDecimal(decimals)
            } else {
                this
            }
            val decimalFiatValue = fiatValue.parseToBigDecimal(decimals)
            fiatValue to decimalFiatValue
        } else {
            "" to null
        }
    }

    private fun SendUiState.emptyState(): SendUiState {
        val isEditState = stateRouterProvider().isEditState
        val amountState = getAmountState(isEditState) ?: return this
        val amountTextField = amountState.amountTextField
        return copyWrapped(
            isEditState = isEditState,
            amountState = amountState.copy(
                isPrimaryButtonEnabled = false,
                amountTextField = amountTextField.copy(
                    value = "",
                    fiatValue = "",
                    cryptoAmount = amountTextField.cryptoAmount.copy(value = BigDecimal.ZERO),
                    fiatAmount = amountTextField.fiatAmount.copy(value = BigDecimal.ZERO),
                    isError = false,
                ),
            ),
        )
    }

    private fun String.checkExceedBalance(amountTextField: SendTextField.AmountField): Boolean {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val currencyCryptoAmount = cryptoCurrencyStatus.value.amount ?: BigDecimal.ZERO
        val currencyFiatAmount = cryptoCurrencyStatus.value.fiatAmount ?: BigDecimal.ZERO
        val fiatDecimal = parseToBigDecimal(amountTextField.fiatAmount.decimals)
        val cryptoDecimal = parseToBigDecimal(amountTextField.cryptoAmount.decimals)
        return if (amountTextField.isFiatValue) {
            fiatDecimal > currencyFiatAmount
        } else {
            cryptoDecimal > currencyCryptoAmount
        }
    }

    private fun getKeyboardAction(isExceedBalance: Boolean, decimalCryptoValue: BigDecimal) =
        if (!isExceedBalance && !decimalCryptoValue.isZero()) {
            ImeAction.Done
        } else {
            ImeAction.None
        }
}
