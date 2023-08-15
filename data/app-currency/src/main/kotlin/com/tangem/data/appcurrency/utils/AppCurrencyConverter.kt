package com.tangem.data.appcurrency.utils

import com.tangem.datasource.api.tangemTech.models.CurrenciesResponse
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.utils.converter.Converter

internal class AppCurrencyConverter : Converter<CurrenciesResponse.Currency, AppCurrency> {

    override fun convert(value: CurrenciesResponse.Currency): AppCurrency {
        return AppCurrency(
            code = value.code,
            name = value.name,
            symbol = value.unit,
        )
    }
}
