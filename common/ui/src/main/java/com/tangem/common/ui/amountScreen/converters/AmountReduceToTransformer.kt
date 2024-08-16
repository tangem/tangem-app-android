package com.tangem.common.ui.amountScreen.converters

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.common.ui.amountScreen.utils.checkExceedBalance
import com.tangem.common.ui.amountScreen.utils.getFiatValue
import com.tangem.common.ui.amountScreen.utils.getKeyboardAction
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.parseBigDecimal
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
    private val value: BigDecimal,
) : Transformer<AmountState> {
    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

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
                error = resourceReference(R.string.send_validation_amount_exceeds_balance),
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