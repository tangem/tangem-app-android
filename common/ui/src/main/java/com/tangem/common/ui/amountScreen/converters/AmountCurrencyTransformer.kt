package com.tangem.common.ui.amountScreen.converters

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.amountScreen.models.AmountState
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.utils.Provider
import com.tangem.utils.isNullOrZero
import com.tangem.utils.transformer.Transformer

/**
 * Selected currency change from crypto currency to app currency and vice versa
 *
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 */
class AmountCurrencyTransformer(
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Transformer<AmountState, Boolean> {
    override fun transform(prevState: AmountState, value: Boolean): AmountState {
        val amountTextField = prevState.amountTextField
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()

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