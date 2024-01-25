package com.tangem.features.send.impl.presentation.state.fields

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.TextReference
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
) : Converter<Unit, SendTextField.AmountField> {

    override fun convert(value: Unit): SendTextField.AmountField {
        val cryptoCurrencyStatus = cryptoCurrencyStatusProvider()
        return SendTextField.AmountField(
            value = "",
            fiatValue = "",
            onValueChange = clickIntents::onAmountValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Number,
            ),
            isFiatValue = false,
            cryptoAmount = BigDecimal.ZERO.convertToAmount(cryptoCurrencyStatus.currency),
            fiatAmount = getAppCurrencyAmount(appCurrencyProvider()),
            isError = false,
            error = TextReference.Res(R.string.swapping_insufficient_funds),
        )
    }

    private fun getAppCurrencyAmount(appCurrency: AppCurrency) = Amount(
        currencySymbol = appCurrency.symbol,
        value = BigDecimal.ZERO,
        decimals = FIAT_DECIMALS,
        type = AmountType.FiatType(appCurrency.code),
    )
}