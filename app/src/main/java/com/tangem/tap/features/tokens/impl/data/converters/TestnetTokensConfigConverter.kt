package com.tangem.tap.features.tokens.impl.data.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.datasource.local.testnet.models.TestnetTokensConfig
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.utils.converter.Converter

/**
 * Converter from data model [TestnetTokensConfig] to list of domain models [Token]
 *
* [REDACTED_AUTHOR]
 */
internal object TestnetTokensConfigConverter : Converter<TestnetTokensConfig, List<Token>> {

    private val coinsResponseConverter = CoinsResponseConverter(needFilterExcluded = false)
    override fun convert(value: TestnetTokensConfig): List<Token> {
        return value.tokens.map { token ->
            Token(
                id = token.id,
                name = token.name,
                symbol = token.symbol,
                iconUrl = coinsResponseConverter.getIconUrl(token.id),
                networks = token.networks?.mapNotNull { network ->
                    val blockchain = Blockchain.fromNetworkId(network.id) ?: return@mapNotNull null

                    Token.Network(
                        id = network.id,
                        blockchain = blockchain,
                        address = network.address,
                        iconUrl = coinsResponseConverter.getIconUrl(network.id),
                        decimalCount = network.decimalCount,
                    )
                }.orEmpty(),
            )
        }
    }
}
