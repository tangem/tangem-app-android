package com.tangem.data.onramp.converters

import com.tangem.datasource.api.onramp.models.response.model.OnrampCurrencyDTO
import com.tangem.domain.onramp.model.OnrampCurrency
import com.tangem.utils.converter.TwoWayConverter

internal class CurrencyConverter : TwoWayConverter<OnrampCurrencyDTO, OnrampCurrency> {

    override fun convert(value: OnrampCurrencyDTO) = OnrampCurrency(
        name = value.name,
        code = value.code,
        image = value.image,
        precision = value.precision,
        unit = value.unit ?: value.code,
    )

    override fun convertBack(value: OnrampCurrency): OnrampCurrencyDTO = OnrampCurrencyDTO(
        name = value.name,
        code = value.code,
        image = value.image,
        precision = value.precision,
        unit = value.unit,
    )
}