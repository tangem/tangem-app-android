package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.OnrampCountryDTO
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.utils.converter.TwoWayConverter

internal class CountryConverter(
    private val currencyConverter: CurrencyConverter,
) : TwoWayConverter<OnrampCountryDTO, OnrampCountry> {

    override fun convert(value: OnrampCountryDTO) = OnrampCountry(
        id = "${value.alpha3}-${value.name}",
        name = value.name,
        code = value.code,
        image = value.image,
        alpha3 = value.alpha3,
        continent = value.continent,
        defaultCurrency = currencyConverter.convert(value.defaultCurrency),
        onrampAvailable = value.onrampAvailable,
    )

    override fun convertBack(value: OnrampCountry): OnrampCountryDTO = OnrampCountryDTO(
        name = value.name,
        code = value.code,
        image = value.image,
        alpha3 = value.alpha3,
        continent = value.continent,
        defaultCurrency = currencyConverter.convertBack(value.defaultCurrency),
        onrampAvailable = value.onrampAvailable,
    )
}