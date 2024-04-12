package com.tangem.tap.features.tokens.impl.data.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.isSupportedInApp
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.utils.converter.Converter

/**
 * Converter from data model [CoinsResponse] to list of domain models [Token]
 *
* [REDACTED_AUTHOR]
 */
internal class CoinsResponseConverter(val needFilterExcluded: Boolean) : Converter<CoinsResponse, List<Token>> {

    override fun convert(value: CoinsResponse): List<Token> {
        return value.coins.map { token ->
            Token(
                id = token.id,
                name = token.name,
                symbol = token.symbol,
                iconUrl = getIconUrl(token.id, value.imageHost),
                networks = token.networks.mapNotNull { network ->
                    val blockchain = Blockchain.fromNetworkId(network.networkId) ?: return@mapNotNull null

                    if (needFilterExcluded && !blockchain.isSupportedInApp()) {
                        return@mapNotNull null
                    }

                    // filter tokens, if contractAddress != null, assume that it is a token
                    if (network.contractAddress != null &&
                        !blockchain.canHandleTokens()
                    ) {
                        return@mapNotNull null
                    }

                    Token.Network(
                        id = network.networkId,
                        blockchain = blockchain,
                        address = network.contractAddress,
                        iconUrl = getIconUrl(network.networkId, value.imageHost),
                        decimalCount = network.decimalCount?.toInt(),
                    )
                },
            )
        }.filter { it.networks.isNotEmpty() }
    }

    fun getIconUrl(id: String, imageHost: String? = null): String {
        return "${imageHost ?: DEFAULT_IMAGE_HOST}large/$id.png"
    }

    private companion object {
        const val DEFAULT_IMAGE_HOST = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/"
    }
}
