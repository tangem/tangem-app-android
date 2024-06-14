package com.tangem.common.ui.amountScreen.converters

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer

/**
 * Selected currency change from crypto currency to app currency and vice versa
 *
 * @property cryptoCurrencyStatus current cryptocurrency status
 * @property value is crypto currency or app currency
 */
class AmountCurrencyTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val value: Boolean,
) : Transformer<AmountState> {

    override fun transform(prevState: AmountState): AmountState {
        if (prevState !is AmountState.Data) return prevState

        val amountTextField = prevState.amountTextField

        val isValidFiatRate = cryptoCurrencyStatus.value.fiatRate.isNullOrZero()
        val isDoneActionEnabled = prevState.isPrimaryButtonEnabled
        return if (amountTextField.isFiatValue == value && !isValidFiatRate) {
            prevState
        } else {
            return prevState.copy(
                amountTextField = amountTextField.copy(
                    isFiatValue = value,
                    isValuePasted = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = if (isDoneActionEnabled) ImeAction.Done else ImeAction.None,
                        keyboardType = KeyboardType.Number,
                    ),
                ),
                selectedButton = prevState.segmentedButtonConfig.indexOfFirst { it.isFiat == value },
            )
        }
    }
}
