package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.utils.converter.Converter

internal class CountryConverter(
    private val currencyConverter: CurrencyConverter,
) : Converter<OnrampCountryDTO, OnrampCountry> {

    override fun convert(value: OnrampCountryDTO) = OnrampCountry(
        name = value.name,
        code = value.code,
        image = value.image,
        alpha3 = value.alpha3,
        continent = value.continent,
        defaultCurrency = currencyConverter.convert(value.defaultCurrency),
        onrampAvailable = value.onrampAvailable,
    )
}