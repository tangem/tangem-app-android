package com.tangem.tap.features.tokens.impl.data.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.domain.tokens.getIconUrl
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.utils.converter.Converter

/**
 * Converter from data model [CoinsResponse] to list of domain models [Token]
 *
 * @author Andrew Khokhlov on 07/04/2023
 */
internal object CoinsResponseConverter : Converter<CoinsResponse, List<Token>> {

    override fun convert(value: CoinsResponse): List<Token> {
        return value.coins.map { token ->
            Token(
                id = token.id,
                name = token.name,
                symbol = token.symbol,
                iconUrl = getIconUrl(token.id, value.imageHost),
                networks = token.networks.mapNotNull { network ->
                    val blockchain = Blockchain.fromNetworkId(network.networkId) ?: return@mapNotNull null

                    Token.Network(
                        id = network.networkId,
                        blockchain = blockchain,
                        address = network.contractAddress,
                        iconUrl = getIconUrl(network.networkId, value.imageHost),
                        decimalCount = network.decimalCount?.toInt(),
                    )
                },
            )
        }
    }
}
