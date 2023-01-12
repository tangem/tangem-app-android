package com.tangem.feature.swap.domain.converters

import com.tangem.feature.swap.domain.models.Currency
import com.tangem.utils.converter.Converter

class CryptoCurrencyConverter : Converter<Currency, com.tangem.lib.crypto.models.Currency> {

    override fun convert(value: Currency): com.tangem.lib.crypto.models.Currency {
        return when (value) {
            is Currency.NonNativeToken -> {
                com.tangem.lib.crypto.models.Currency.NonNativeToken(
                    id = value.id,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.networkId,
                    contractAddress = value.contractAddress,
                    decimalCount = value.decimalCount,
                )
            }
            is Currency.NativeToken -> {
                com.tangem.lib.crypto.models.Currency.NativeToken(
                    id = value.id,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.networkId,
                )
            }
        }
    }
}
