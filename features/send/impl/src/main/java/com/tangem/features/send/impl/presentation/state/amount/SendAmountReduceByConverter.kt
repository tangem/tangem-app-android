package com.tangem.features.send.impl.presentation.state.amount

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

internal class SendAmountReduceByConverter(
    private val stateRouterProvider: Provider<StateRouter>,
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<SendAmountReduceByConverter.ReduceByData, SendUiState> {
    override fun convert(value: ReduceByData): SendUiState {
        val state = currentStateProvider()
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val isEditState = stateRouterProvider().isEditState
        val amountState = state.getAmountState(isEditState) ?: return state
        val amountTextField = amountState.amountTextField
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals
        val amountValue = amountState.amountTextField.cryptoAmount.value ?: return state

        val decimalCryptoValue = amountValue.minus(value.reduceAmountByDiff)
        val cryptoValue = decimalCryptoValue.parseBigDecimal(cryptoDecimals)
        val (fiatValue, decimalFiatValue) = cryptoValue.getFiatValue(
            fiatRate = cryptoCurrencyStatus.value.fiatRate,
            isFiatValue = false,
            decimals = fiatDecimals,
        )

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(cryptoCurrencyStatus, amountTextField)
        val isZero = if (amountTextField.isFiatValue) decimalFiatValue.isNullOrZero() else decimalCryptoValue.isZero()
        return state.copyWrapped(
            isEditState = isEditState,
            sendState = state.sendState?.copy(
                reduceAmountBy = value.reduceAmountBy,
            ),
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

    internal data class ReduceByData(
        val reduceAmountBy: BigDecimal,
        val reduceAmountByDiff: BigDecimal,
    )
}
