package com.tangem.data.transaction.converters

import com.tangem.datasource.api.gasless.models.GaslessTokenDTO
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.utils.converter.Converter

internal class GaslessTokenDtoToCryptoCurrencyConverter : Converter<GaslessTokenDTO, CryptoCurrency> {

    override fun convert(value: GaslessTokenDTO): CryptoCurrency {
        TODO("implement conversion logic here")
    }
}