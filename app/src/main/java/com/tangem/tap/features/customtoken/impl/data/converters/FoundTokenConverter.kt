package com.tangem.tap.features.customtoken.impl.data.converters

import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.utils.converter.Converter

/**
 * Converter between data model [CoinsResponse.Coin] and domain model [FoundToken]
 *
[REDACTED_AUTHOR]
 */
object FoundTokenConverter : Converter<CoinsResponse.Coin, FoundToken> {

    override fun convert(value: CoinsResponse.Coin): FoundToken {
        return FoundToken(
            id = value.id,
            name = value.name,
            symbol = value.symbol,
            isActive = value.active,
            network = value.networks.firstOrNull()?.let { network ->
                FoundToken.Network(
                    id = network.networkId,
                    address = requireNotNull(network.contractAddress),
                    decimalCount = requireNotNull(network.decimalCount).toString(),
                )
            } ?: error("Found token networks is empty"),
        )
    }
}