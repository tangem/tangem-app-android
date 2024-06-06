package com.tangem.common.ui.amountScreen.converters.field

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.common.ui.R
import com.tangem.common.ui.amountScreen.AmountScreenClickIntents
import com.tangem.common.ui.amountScreen.models.AmountFieldModel
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.convertToAmount
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import com.tangem.utils.isNullOrZero
import java.math.BigDecimal

/**
 * Converts initial [String] to [AmountField]
 *
 * @property clickIntents amount screen clicks
 * @property appCurrencyProvider selected app currency provider
 * @property cryptoCurrencyStatusProvider current cryptocurrency status provider
 */
class AmountFieldConverter(
    private val clickIntents: AmountScreenClickIntents,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<String, AmountFieldModel> {

    override fun convert(value: String): AmountFieldModel {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val cryptoDecimal = value.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val cryptoAmount = cryptoDecimal.convertToAmount(cryptoCurrencyStatus.currency)
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        val (fiatValue, fiatDecimal) = when {
            fiatRate.isNullOrZero() -> "" to null
            value.isEmpty() -> "" to BigDecimal.ZERO
            else -> {
                val fiatDecimal = fiatRate?.multiply(cryptoDecimal)
                val fiatValue = fiatDecimal?.parseBigDecimal(FIAT_DECIMALS).orEmpty()
                fiatValue to fiatDecimal
            }
        }
        val isDoneActionEnabled = !cryptoDecimal.isNullOrZero()
        return AmountFieldModel(
            value = value,
            fiatValue = fiatValue,
            onValueChange = clickIntents::onAmountValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = if (isDoneActionEnabled) ImeAction.Done else ImeAction.None,
                keyboardType = KeyboardType.Number,
            ),
            keyboardActions = KeyboardActions(
                onDone = { clickIntents.onAmountNext() },
            ),
            isFiatValue = false,
            cryptoAmount = cryptoAmount,
            fiatAmount = getAppCurrencyAmount(fiatDecimal, appCurrencyProvider()),
            isError = false,
            error = TextReference.Res(R.string.send_validation_amount_exceeds_balance),
            isFiatUnavailable = fiatRate == null,
            isValuePasted = false,
            onValuePastedTriggerDismiss = clickIntents::onAmountPasteTriggerDismiss,
        )
    }

    private fun getAppCurrencyAmount(fiatValue: BigDecimal?, appCurrency: AppCurrency) = Amount(
        currencySymbol = appCurrency.symbol,
        value = fiatValue,
        decimals = FIAT_DECIMALS,
        type = AmountType.FiatType(appCurrency.code),
    )

    private companion object {
        private const val FIAT_DECIMALS = 2
    }
}