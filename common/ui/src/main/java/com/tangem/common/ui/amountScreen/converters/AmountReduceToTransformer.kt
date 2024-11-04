package com.tangem.common.ui.amountScreen.converters

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.models.EnterAmountBoundary
import com.tangem.common.ui.amountScreen.utils.checkExceedBalance
import com.tangem.common.ui.amountScreen.utils.getFiatValue
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.core.ui.utils.parseToBigDecimal
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer
import java.math.BigDecimal

/**
 * Reduces amount to specific value
 *
 * @property cryptoCurrencyStatus current cryptocurrency status
 * @property value reduced to value
 */
class AmountReduceToTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val minimumTransactionAmount: EnterAmountBoundary?,
    private val value: BigDecimal,
) : Transformer<AmountState> {
    private val maxEnterAmountConverter = MaxEnterAmountConverter()

    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

        val amountTextField = prevState.amountTextField
        val cryptoDecimals = amountTextField.cryptoAmount.decimals
        val fiatDecimals = amountTextField.fiatAmount.decimals

        val cryptoValue = value.parseBigDecimal(cryptoDecimals)
        val decimalCryptoValue = cryptoValue.parseToBigDecimal(cryptoDecimals)
        val (fiatValue, decimalFiatValue) = cryptoValue.getFiatValue(
            fiatRate = cryptoCurrencyStatus.value.fiatRate,
            isFiatValue = false,
            decimals = fiatDecimals,
        )

        val maxEnterAmount = maxEnterAmountConverter.convert(cryptoCurrencyStatus)

        val checkValue = if (amountTextField.isFiatValue) fiatValue else cryptoValue
        val isExceedBalance = checkValue.checkExceedBalance(maxEnterAmount, amountTextField)
        val isLessThanMinimumIfProvided = minimumTransactionAmount?.amount?.let { decimalCryptoValue < it } ?: false
        val isZero = if (amountTextField.isFiatValue) decimalFiatValue.isNullOrZero() else value.isNullOrZero()
        val isCheckFailed = isExceedBalance || isLessThanMinimumIfProvided
        return prevState.copy(
            isPrimaryButtonEnabled = !isZero && !isCheckFailed,
            amountTextField = amountTextField.copy(
                value = cryptoValue,
                fiatValue = fiatValue,
                isError = isCheckFailed,
                error = when {
                    isExceedBalance -> resourceReference(R.string.send_validation_amount_exceeds_balance)
                    isLessThanMinimumIfProvided -> {
                        val minimumAmount = minimumTransactionAmount
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
                cryptoAmount = amountTextField.cryptoAmount.copy(value = value),
                fiatAmount = amountTextField.fiatAmount.copy(value = decimalFiatValue),
                keyboardOptions = KeyboardOptions(
                    imeAction = getKeyboardAction(isCheckFailed, decimalCryptoValue),
                    keyboardType = KeyboardType.Number,
                ),
            ),
        )
    }
}