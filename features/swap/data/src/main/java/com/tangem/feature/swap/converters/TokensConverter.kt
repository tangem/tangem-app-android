package com.tangem.feature.swap.converters

import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.feature.swap.domain.models.domain.Currency
import com.tangem.utils.converter.Converter

class TokensConverter : Converter<CoinsResponse.Coin, Currency> {

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
                logoUrl = getSmallIconUrl(value.id),
            )
        } else {
            Currency.NativeToken(
                id = value.id,
                name = value.name,
                symbol = value.symbol,
                networkId = network.networkId,
                logoUrl = getSmallIconUrl(value.id),
            )
        }
    }

    private fun getSmallIconUrl(coin: String): String {
        return "$DEFAULT_IMAGE_HOST$LARGE_ICON_PATH/$coin.png"
    }

    companion object {
        private const val DEFAULT_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/"
        private const val LARGE_ICON_PATH = "large"
    }
}
