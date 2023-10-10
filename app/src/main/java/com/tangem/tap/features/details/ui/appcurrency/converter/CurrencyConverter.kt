package com.tangem.tap.features.details.ui.appcurrency.converter

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.tap.features.details.ui.appcurrency.AppCurrencySelectorState
import com.tangem.utils.converter.Converter

internal class CurrencyConverter : Converter<AppCurrency, AppCurrencySelectorState.Currency> {

    override fun convert(value: AppCurrency): AppCurrencySelectorState.Currency {
        val fullCurrencyName = with(value) { "$name ($code) — $symbol" }

        return AppCurrencySelectorState.Currency(value.code, fullCurrencyName)
    }
}