package com.tangem.tap.features.tokens.impl.data.converters

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.local.testnet.models.TestnetTokensConfig
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.tap.domain.tokens.getIconUrl
import com.tangem.tap.features.tokens.impl.domain.models.Token
import com.tangem.utils.converter.Converter

/**
 * Converter from data model [TestnetTokensConfig] to list of domain models [Token]
 *
 * @author Andrew Khokhlov on 07/04/2023
 */
internal object TestnetTokensConfigConverter : Converter<TestnetTokensConfig, List<Token>> {

    override fun convert(value: TestnetTokensConfig): List<Token> {
        return value.tokens.map { token ->
            Token(
                id = token.id,
                name = token.name,
                symbol = token.symbol,
                iconUrl = getIconUrl(id = token.id, imageHost = null),
                networks = token.networks?.mapNotNull { network ->
                    val blockchain = Blockchain.fromNetworkId(network.id) ?: return@mapNotNull null

                    Token.Network(
                        id = network.id,
                        blockchain = blockchain,
                        address = network.address,
                        iconUrl = getIconUrl(id = network.id, imageHost = null),
                        decimalCount = network.decimalCount,
                    )
                }.orEmpty(),
            )
        }
    }
}
