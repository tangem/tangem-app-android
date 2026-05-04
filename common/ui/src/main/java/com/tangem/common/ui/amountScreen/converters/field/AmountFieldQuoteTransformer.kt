package com.tangem.common.ui.amountScreen.converters.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.amountScreen.utils.getAmountValidationError
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * @property cryptoAmount the crypto amount to set, or `null` to clear the field
 * @property maxEnterAmount maximum allowed amount boundary (for balance validation)
 * @property minimumTransactionAmount minimum allowed amount boundary (optional)
 */
class AmountFieldQuoteTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val cryptoAmount: BigDecimal?,
    private val maxEnterAmount: EnterAmountBoundary,
    private val minimumTransactionAmount: EnterAmountBoundary?,
) : Transformer<AmountState> {

    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState
        if (cryptoAmount == null) return prevState.emptyState(maxEnterAmount.fiatRate)

        val textField = prevState.amountTextField
        val cryptoDecimals = textField.cryptoAmount.decimals
        val fiatDecimals = textField.fiatAmount.decimals
        val fiatRate = maxEnterAmount.fiatRate
        val cryptoString = cryptoAmount.parseBigDecimal(cryptoDecimals)
        val fiatValue = fiatRate?.let { cryptoAmount.multiply(it).setScale(fiatDecimals, RoundingMode.HALF_UP) }
        val fiatString = fiatValue?.parseBigDecimal(fiatDecimals).orEmpty()

        val isExceedBalance = if (textField.isFiatValue) {
            val maxFiat = maxEnterAmount.fiatAmount
            maxFiat != null && fiatValue != null && fiatValue > maxFiat
        } else {
            val maxCrypto = maxEnterAmount.amount
            maxCrypto != null && cryptoAmount > maxCrypto
        }
        val isLessThanMinimum = minimumTransactionAmount?.amount?.let { cryptoAmount < it } == true
        val isZero = cryptoAmount.isNullOrZero()
        val isCheckFailed = isExceedBalance || isLessThanMinimum

        return prevState.copy(
            isPrimaryButtonEnabled = !isZero && !isCheckFailed,
            reduceAmountBy = BigDecimal.ZERO,
            amountTextField = textField.copy(
                value = cryptoString,
                fiatValue = fiatString,
                cryptoAmount = textField.cryptoAmount.copy(value = cryptoAmount),
                fiatAmount = textField.fiatAmount.copy(value = fiatValue),
                isError = isCheckFailed,
                error = getAmountValidationError(
                    isExceedBalance = isExceedBalance,
                    isLessThanMinimum = isLessThanMinimum,
                    minimumTransactionAmount = minimumTransactionAmount,
                    cryptoCurrency = cryptoCurrencyStatus.currency,
                ),
                keyboardOptions = KeyboardOptions(
                    imeAction = getKeyboardAction(isCheckFailed, cryptoAmount),
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }

    private fun AmountState.Data.emptyState(fiatRate: BigDecimal?): AmountState.Data {
        return copy(
            isPrimaryButtonEnabled = false,
            reduceAmountBy = BigDecimal.ZERO,
            amountTextField = amountTextField.copy(
                value = "",
                fiatValue = "",
                cryptoAmount = amountTextField.cryptoAmount.copy(value = BigDecimal.ZERO),
                fiatAmount = amountTextField.fiatAmount.copy(value = if (fiatRate != null) BigDecimal.ZERO else null),
                isError = false,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.None,
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }
}