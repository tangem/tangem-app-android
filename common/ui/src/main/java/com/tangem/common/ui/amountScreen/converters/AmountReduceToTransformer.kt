package com.tangem.common.ui.amountScreen.converters

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.checkExceedBalance
import com.tangem.common.ui.amountScreen.utils.getFiatValue
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Reduces amount to specific value
 *
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 */
class AmountReduceToTransformer(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Transformer<AmountState, BigDecimal> {
    override fun transform(prevState: AmountState, value: BigDecimal): AmountState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val amountTextField = prevState.amountTextField
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val cryptoValue = value.parseBigDecimal(cryptoDecimals)
        val (fiatValue, decimalFiatValue) = cryptoValue.getFiatValue(
            fiatRate = cryptoCurrencyStatus.value.fiatRate,
            isFiatValue = false,
            decimals = fiatDecimals,
        )

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(cryptoCurrencyStatus, amountTextField)
        val isZero = if (amountTextField.isFiatValue) decimalFiatValue.isNullOrZero() else value.isNullOrZero()
        return prevState.copy(
            isPrimaryButtonEnabled = !isExceedBalance && !isZero,
            amountTextField = amountTextField.copy(
                value = cryptoValue,
                fiatValue = fiatValue,
                isError = isExceedBalance,
                cryptoAmount = amountTextField.cryptoAmount.copy(value = value),
                fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                keyboardOptions = KeyboardOptions(
                    imeAction = getKeyboardAction(isExceedBalance, value),
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }
}
