package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.utils.converter.Converter

class CurrencyConverter : Converter<OnrampCurrencyDTO, OnrampCurrency> {

    override fun convert(value: OnrampCurrencyDTO) = OnrampCurrency(
        name = value.name,
        code = value.code,
        image = value.image,
        precision = value.precision,
    )
}