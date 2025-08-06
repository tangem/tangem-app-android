package com.tangem.common.ui.amountScreen.converters.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.amountScreen.utils.checkExceedBalance
import com.tangem.common.ui.amountScreen.utils.getCryptoValue
import com.tangem.common.ui.amountScreen.utils.getFiatValue
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Amount value change
 *
 * @property maxEnterAmount max amount to enter
 * @property value amount value
 */
class AmountFieldChangeTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val maxEnterAmount: EnterAmountBoundary,
    private val minimumTransactionAmount: EnterAmountBoundary?,
    private val value: String,
) : Transformer<AmountState> {

    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

        val amountTextField = prevState.amountTextField

        if (value.isEmpty()) return prevState.emptyState()
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val trimmedValue = value.trim()
        val cryptoValue = trimmedValue.getCryptoValue(
            fiatRate = maxEnterAmount.fiatRate,
            isFiatValue = amountTextField.isFiatValue,
            decimals = cryptoDecimals,
        )
        val decimalCryptoValue = cryptoValue.parseToBigDecimal(cryptoDecimals)
        val (fiatValue, decimalFiatValue) = trimmedValue.getFiatValue(
            fiatRate = maxEnterAmount.fiatRate,
            isFiatValue = amountTextField.isFiatValue,
            decimals = fiatDecimals,
        )

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(maxEnterAmount, amountTextField)
        val isLessThanMinimumIfProvided = minimumTransactionAmount?.amount?.let { decimalCryptoValue < it } == true
        val isZero = if (amountTextField.isFiatValue) {
            decimalFiatValue.isNullOrZero()
        } else {
            decimalCryptoValue.isNullOrZero()
        }
        val isCheckFailed = isExceedBalance || isLessThanMinimumIfProvided
        return prevState.copy(
            isPrimaryButtonEnabled = !isZero && !isCheckFailed,
            reduceAmountBy = BigDecimal.ZERO,
            amountTextField = amountTextField.copy(
                value = cryptoValue,
                fiatValue = fiatValue,
                isError = isCheckFailed,
                error = when {
                    isExceedBalance -> resourceReference(R.string.send_validation_amount_exceeds_balance)
                    isLessThanMinimumIfProvided -> {
                        val minimumAmount = minimumTransactionAmount?.amount.format {
                            crypto(cryptoCurrencyStatus.currency)
                        }

                        resourceReference(
                            R.string.transfer_notification_invalid_minimum_transaction_amount_text,
                            wrappedList(minimumAmount, minimumAmount),
                        )
                    }
                    else -> TextReference.EMPTY
                },
                cryptoAmount = amountTextField.cryptoAmount.copy(value = decimalCryptoValue),
                fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                keyboardOptions = KeyboardOptions(
                    imeAction = getKeyboardAction(isCheckFailed, decimalCryptoValue),
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }

    private fun AmountState.Data.emptyState(): AmountState.Data {
        return copy(
            isPrimaryButtonEnabled = false,
            reduceAmountBy = BigDecimal.ZERO,
            amountTextField = amountTextField.copy(
                value = "",
                fiatValue = "",
                cryptoAmount = amountTextField.cryptoAmount.copy(value = BigDecimal.ZERO),
                fiatAmount = amountTextField.fiatAmount.copy(value = BigDecimal.ZERO),
                isError = false,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.None,
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }
}