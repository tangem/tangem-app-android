package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.RoundingMode

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
        val amountTextField = amountState.amountTextField

        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals
        val decimalCryptoValue = cryptoCurrencyStatus.value.amount
        val decimalFiatValue = cryptoCurrencyStatus.value.fiatAmount

        if (decimalCryptoValue.isNullOrZero()) return state

        val isDoneActionEnabled = !decimalCryptoValue.isNullOrZero()
        val cryptoValue = decimalCryptoValue?.parseBigDecimal(cryptoDecimals).orEmpty()
        val fiatValue = decimalFiatValue?.parseBigDecimal(fiatDecimals, roundingMode = RoundingMode.HALF_UP).orEmpty()
        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(reduceAmountBy = null),
            amountState = amountState.copy(
                isPrimaryButtonEnabled = true,
                amountTextField = amountTextField.copy(
                    isValuePasted = true,
                    value = cryptoValue,
                    fiatValue = fiatValue,
                    isError = false,
                    cryptoAmount = amountTextField.cryptoAmount.copy(value = decimalCryptoValue),
                    fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (isDoneActionEnabled) ImeAction.Done else ImeAction.None,
                        keyboardType = KeyboardType.Number,
                    ),
                ),
            ),
        )
    }
}
