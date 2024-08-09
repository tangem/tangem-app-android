package com.tangem.data.tokens.paging

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.data.tokens.utils.getNetworkStandardType
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Token
import com.tangem.utils.converter.Converter

/**
 * Converter from data model [CoinsResponse] to list of domain models [Token]
 */
internal object CoinsResponseConverter : Converter<CoinsData, List<Token>> {

    override fun convert(value: CoinsData): List<Token> {
        return value.coins.map { coin ->
            val id = CryptoCurrency.ID(
                CryptoCurrency.ID.Prefix.TOKEN_PREFIX,
                CryptoCurrency.ID.Body.NetworkId(coin.id),
                CryptoCurrency.ID.Suffix.RawID(coin.id),
            )
            val quote = value.quotes.find { it.rawCurrencyId == id.rawCurrencyId }
            Token(
                id = id.rawCurrencyId ?: coin.name,
                name = coin.name,
                symbol = coin.symbol,
                iconUrl = getIconUrl(coin.id, value.imageHost),
                isAvailable = coin.active,
                networks = coin.networks.mapNotNull { network ->
                    val blockchain = Blockchain.fromNetworkId(network.networkId) ?: return@mapNotNull null
                    Token.Network(
                        networkId = network.networkId,
                        standardType = getNetworkStandardType(blockchain).name,
                        name = blockchain.getNetworkName(),
                        address = network.contractAddress,
                        iconUrl = getIconUrl(network.networkId, value.imageHost),
                        decimalCount = network.decimalCount?.toInt(),
                    )
                },
                quote = quote,
            )
        }
    }
}

internal fun getIconUrl(id: String, imageHost: String? = null): String {
    return "${imageHost ?: DEFAULT_IMAGE_HOST}large/$id.png"
}

private const val DEFAULT_IMAGE_HOST =
    "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/"
