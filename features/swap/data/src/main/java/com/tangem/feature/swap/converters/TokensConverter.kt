package com.tangem.feature.swap.converters

import com.tangem.datasource.api.tangemTech.CoinsResponse
import com.tangem.feature.swap.domain.models.Currency
import com.tangem.utils.converter.Converter
import javax.inject.Inject

class TokensConverter @Inject constructor() : Converter<CoinsResponse.Coin, Currency> {

    override fun convert(value: CoinsResponse.Coin): Currency {
        val network = value.networks.first()
        return if (network.contractAddress != null && network.decimalCount != null) {
            Currency.NonNativeToken(
                id = value.id,
                name = value.name,
                symbol = value.symbol,
                networkId = network.networkId,
                contractAddress = network.contractAddress!!,
                decimalCount = network.decimalCount!!.intValueExact(),
                logoUrl = "",//todo add logo
            )
        } else {
            Currency.NativeToken(
                id = value.id,
                name = value.name,
                symbol = value.symbol,
                networkId = network.networkId,
                logoUrl = "", //todo add logo
            )
        }
    }
}
