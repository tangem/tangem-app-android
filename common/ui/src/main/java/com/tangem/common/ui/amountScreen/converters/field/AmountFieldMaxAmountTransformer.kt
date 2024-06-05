package com.tangem.common.ui.amountScreen.converters.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.RoundingMode

/**
 * Selects maximum amount value
 *
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 */
class AmountFieldMaxAmountTransformer(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Transformer<AmountState, Unit> {

    override fun transform(prevState: AmountState, value: Unit): AmountState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val amountTextField = prevState.amountTextField

        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals
        val decimalCryptoValue = cryptoCurrencyStatus.value.amount
        val decimalFiatValue = cryptoCurrencyStatus.value.fiatAmount

        if (decimalCryptoValue.isNullOrZero()) return prevState

        val isDoneActionEnabled = !decimalCryptoValue.isNullOrZero()
        val cryptoValue = decimalCryptoValue?.parseBigDecimal(cryptoDecimals).orEmpty()
        val fiatValue = decimalFiatValue?.parseBigDecimal(fiatDecimals, roundingMode = RoundingMode.HALF_UP).orEmpty()
        return prevState.copy(
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
        )
    }
}