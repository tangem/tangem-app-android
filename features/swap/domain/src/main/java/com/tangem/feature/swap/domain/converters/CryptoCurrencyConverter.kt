package com.tangem.feature.swap.domain.converters

import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.utils.converter.TwoWayConverter
import com.tangem.lib.crypto.models.Currency as CryptoCurrency

class CryptoCurrencyConverter : TwoWayConverter<Currency, CryptoCurrency> {

    override fun convert(value: Currency): CryptoCurrency {
        return when (value) {
            is Currency.NonNativeToken -> {
                CryptoCurrency.NonNativeToken(
                    id = value.id,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.networkId,
                    contractAddress = value.contractAddress,
                    decimalCount = value.decimalCount,
                )
            }
            is Currency.NativeToken -> {
                CryptoCurrency.NativeToken(
                    id = value.id,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.networkId,
                )
            }
        }
    }

    override fun convertBack(value: CryptoCurrency): Currency {
        return when (value) {
            is CryptoCurrency.NonNativeToken -> {
                Currency.NonNativeToken(
                    id = value.id,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.networkId,
                    contractAddress = value.contractAddress,
                    decimalCount = value.decimalCount,
                    logoUrl = "",
                )
            }
            is CryptoCurrency.NativeToken -> {
                Currency.NativeToken(
                    id = value.id,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.networkId,
                    logoUrl = "",
                )
            }
        }
    }
}
