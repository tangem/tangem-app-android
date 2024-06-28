package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.amount.checkExceedBalance
import com.tangem.features.send.impl.presentation.state.amount.getCryptoValue
import com.tangem.features.send.impl.presentation.state.amount.getFiatValue
import com.tangem.features.send.impl.presentation.state.amount.getKeyboardAction
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

internal class SendAmountFieldChangeConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<String, SendUiState> {
    override fun convert(value: String): SendUiState {
        val state = currentStateProvider()
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state
        val amountTextField = amountState.amountTextField

        if (value.isEmpty()) return state.emptyState()
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val trimmedValue = value.trim()
        val cryptoValue = trimmedValue.getCryptoValue(
            fiatRate = cryptoCurrencyStatus.value.fiatRate,
            isFiatValue = amountTextField.isFiatValue,
            decimals = cryptoDecimals,
        )
        val decimalCryptoValue = cryptoValue.parseToBigDecimal(cryptoDecimals)
        val (fiatValue, decimalFiatValue) = trimmedValue.getFiatValue(
            fiatRate = cryptoCurrencyStatus.value.fiatRate,
            isFiatValue = amountTextField.isFiatValue,
            decimals = fiatDecimals,
        )

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(cryptoCurrencyStatus, amountTextField)
        val isZero = if (amountTextField.isFiatValue) decimalFiatValue.isNullOrZero() else decimalCryptoValue.isZero()
        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(reduceAmountBy = null),
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
}