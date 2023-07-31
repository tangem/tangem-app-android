package com.tangem.data.tokens.utils

import com.tangem.blockchain.common.Blockchain
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.demo.DemoConfig
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.tokens.model.Token
import timber.log.Timber
import com.tangem.blockchain.common.Token as SdkToken

internal class ResponseTokensFactory(private val demoConfig: DemoConfig) {

    fun createTokens(response: UserTokensResponse, card: CardDTO): Set<Token> {
        return response.tokens.mapNotNull { createToken(it, card) }.toSet()
    }

    private fun createToken(token: UserTokensResponse.Token, card: CardDTO): Token? {
        var blockchain = Blockchain.fromNetworkId(token.networkId)
        if (blockchain == null) {
            Timber.e("Unable to find a blockchain with the network ID: ${token.networkId}")
            return null
        }

        if (demoConfig.isDemoCardId(card.cardId)) {
            blockchain = blockchain.getTestnetVersion() ?: blockchain
        }

        val sdkToken = createSdkToken(token)
        val (tokenId, iconUrl) = if (sdkToken == null) {
            getCoinId(blockchain) to getCoinIconUrl(blockchain)
        } else {
            getTokenId(blockchain, sdkToken) to getTokenIconUrl(blockchain, sdkToken)
        }

        return Token(
            id = tokenId,
            networkId = getNetworkId(blockchain),
            name = token.name,
            symbol = token.symbol,
            decimals = token.decimals,
            iconUrl = iconUrl,
            contractAddress = token.contractAddress,
            derivationPath = token.derivationPath,
            isCustom = isCustomToken(tokenId),
        )
    }

    private fun createSdkToken(token: UserTokensResponse.Token): SdkToken? {
        return token.contractAddress?.let { contractAddress ->
            SdkToken(
                name = token.name,
                symbol = token.symbol,
                contractAddress = contractAddress,
                decimals = token.decimals,
                id = token.id,
            )
        }
    }
}
