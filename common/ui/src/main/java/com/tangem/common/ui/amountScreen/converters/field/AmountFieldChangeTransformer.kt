package com.tangem.common.ui.amountScreen.converters.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.checkExceedBalance
import com.tangem.common.ui.amountScreen.utils.getCryptoValue
import com.tangem.common.ui.amountScreen.utils.getFiatValue
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Amount value change
 *
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 */
class AmountFieldChangeTransformer(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Transformer<AmountState, String> {

    override fun transform(prevState: AmountState, value: String): AmountState {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val amountTextField = prevState.amountTextField

        if (value.isEmpty()) return prevState.emptyState()
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
        val isZero = if (amountTextField.isFiatValue) {
            decimalFiatValue.isNullOrZero()
        } else {
            decimalCryptoValue.isNullOrZero()
        }
        return prevState.copy(
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
        )
    }

    private fun AmountState.emptyState(): AmountState {
        return copy(
            isPrimaryButtonEnabled = false,
            amountTextField = amountTextField.copy(
                value = "",
                fiatValue = "",
                cryptoAmount = amountTextField.cryptoAmount.copy(value = BigDecimal.ZERO),
                fiatAmount = amountTextField.fiatAmount.copy(value = BigDecimal.ZERO),
                isError = false,
            ),
        )
    }
}