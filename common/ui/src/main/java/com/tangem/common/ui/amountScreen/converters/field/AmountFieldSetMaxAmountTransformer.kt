package com.tangem.common.ui.amountScreen.converters.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.extensions.isZero
import com.tangem.utils.transformer.Transformer
import java.math.RoundingMode

/**
 * Selects maximum amount value
 *
 * @property maxAmount maximum enter amount
 */
class AmountFieldSetMaxAmountTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val maxAmount: EnterAmountBoundary,
    private val minAmount: EnterAmountBoundary?,
) : Transformer<AmountState> {

    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

        val amountTextField = prevState.amountTextField

        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals
        val decimalCryptoValue = maxAmount.amount
        val decimalFiatValue = maxAmount.fiatAmount

        if (decimalCryptoValue == null || decimalCryptoValue.isZero()) return prevState

        val cryptoValue = decimalCryptoValue.parseBigDecimal(cryptoDecimals)
        val fiatValue = decimalFiatValue?.parseBigDecimal(fiatDecimals, roundingMode = RoundingMode.HALF_UP).orEmpty()
        val isLessThanMinimumIfProvided = minAmount?.amount?.let { decimalCryptoValue < it } ?: false
        return prevState.copy(
            isPrimaryButtonEnabled = !isLessThanMinimumIfProvided,
            amountTextField = amountTextField.copy(
                isValuePasted = true,
                value = cryptoValue,
                fiatValue = fiatValue,
                isError = isLessThanMinimumIfProvided,
                error = when {
                    isLessThanMinimumIfProvided -> {
                        val minimumAmount = minAmount
                            ?.amount
                            ?.format { crypto(cryptoCurrencyStatus.currency) }
                            .orEmpty()
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
                    imeAction = getKeyboardAction(isLessThanMinimumIfProvided, decimalCryptoValue),
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }
}