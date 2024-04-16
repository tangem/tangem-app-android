package com.tangem.features.send.impl.presentation.state.amount

import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendUiState
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

internal class SendAmountSubtractConverter(
    private val currentStateProvider: Provider<SendUiState>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendUiState> {
    override fun convert(value: Unit): SendUiState {
        val state = currentStateProvider()
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val amountState = state.amountState ?: return state
        val feeState = state.feeState ?: return state
        val feeValue = feeState.fee?.amount?.value ?: return state
        val amountTextField = amountState.amountTextField
        val amountValue = amountTextField.cryptoAmount.value ?: return state
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val feeDiff = amountState.subtractedFee?.let { feeValue.minus(it) } ?: feeValue
        val decimalCryptoValue = amountValue.minus(feeDiff)

        if (decimalCryptoValue < BigDecimal.ZERO) return state

        val decimalFiatValue = decimalCryptoValue.multiply(fiatRate)
        val cryptoValue = decimalCryptoValue.parseBigDecimal(cryptoDecimals)
        val fiatValue = decimalFiatValue.parseBigDecimal(fiatDecimals)

        return state.copy(
            sendState = state.sendState?.copy(isSubtract = true),
            amountState = amountState.copy(
                subtractedFee = feeValue,
                amountTextField = amountTextField.copy(
                    value = cryptoValue,
                    fiatValue = fiatValue,
                    cryptoAmount = amountTextField.cryptoAmount.copy(value = decimalCryptoValue),
                    fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                ),
            ),
        )
    }
}