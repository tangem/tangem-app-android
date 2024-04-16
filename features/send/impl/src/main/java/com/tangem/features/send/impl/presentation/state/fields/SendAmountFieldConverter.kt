package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.extensions.toBigDecimalOrDefault
import com.tangem.common.extensions.isZero
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.Amount
import com.tangem.domain.tokens.model.AmountType
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.convertToAmount
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

private const val FIAT_DECIMALS = 2

internal class SendAmountFieldConverter(
    private val clickIntents: SendClickIntents,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
    private val appCurrencyProvider: Provider<AppCurrency>,
) : Converter<String, SendTextField.AmountField> {

    override fun convert(value: String): SendTextField.AmountField {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        val cryptoDecimal = value.toBigDecimalOrDefault()
        val cryptoAmount = cryptoDecimal.convertToAmount(cryptoCurrencyStatus.currency)
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        val (fiatValue, fiatDecimal) = if (value.isEmpty()) {
            "" to BigDecimal.ZERO
        } else {
            val fiatDecimal = fiatRate?.multiply(cryptoDecimal) ?: BigDecimal.ZERO
            val fiatValue = fiatDecimal.parseBigDecimal(FIAT_DECIMALS)
            fiatValue to fiatDecimal
        }
        val isDoneActionEnabled = !cryptoDecimal.isZero()
        return SendTextField.AmountField(
            value = value,
            fiatValue = fiatValue,
            onValueChange = clickIntents::onAmountValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = if (isDoneActionEnabled) ImeAction.Done else ImeAction.None,
                keyboardType = KeyboardType.Number,
            ),
            keyboardActions = KeyboardActions(onDone = { clickIntents.onNextClick() }),
            isFiatValue = false,
            cryptoAmount = cryptoAmount,
            fiatAmount = getAppCurrencyAmount(fiatDecimal, appCurrencyProvider()),
            isError = false,
            error = TextReference.Res(R.string.swapping_insufficient_funds),
            isFiatUnavailable = fiatRate == null,
        )
    }

    private fun getAppCurrencyAmount(fiatValue: BigDecimal, appCurrency: AppCurrency) = Amount(
        currencySymbol = appCurrency.symbol,
        value = fiatValue,
        decimals = FIAT_DECIMALS,
        type = AmountType.FiatType(appCurrency.code),
    )
}