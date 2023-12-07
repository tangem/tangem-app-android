package com.tangem.feature.swap.domain.converters

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.lib.crypto.models.Currency
import com.tangem.utils.converter.Converter

class SwapCurrencyConverter : Converter<CryptoCurrency, Currency> {

    override fun convert(value: CryptoCurrency): Currency {
        return when (value) {
            is CryptoCurrency.Token -> {
                Currency.NonNativeToken(
                    id = value.id.value,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.network.id.value,
                    contractAddress = value.contractAddress,
                    decimalCount = value.decimals,
                )
            }
            is CryptoCurrency.Coin -> {
                Currency.NativeToken(
                    id = value.id.value,
                    name = value.name,
                    symbol = value.symbol,
                    networkId = value.network.id.value,
                )
            }
        }
    }
}