package com.tangem.data.tokens.utils

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.model.Token

internal class UserTokensResponseFactory {

    fun createUserTokensResponse(
        tokens: Set<Token>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ): UserTokensResponse {
        return UserTokensResponse(
            tokens = tokens.map(::createResponseToken),
            group = if (isGroupedByNetwork) {
                UserTokensResponse.GroupType.NETWORK
            } else {
                UserTokensResponse.GroupType.NONE
            },
            sort = if (isSortedByBalance) {
                UserTokensResponse.SortType.BALANCE
            } else {
                UserTokensResponse.SortType.MANUAL
            },
        )
    }

    private fun createResponseToken(domainToken: Token): UserTokensResponse.Token {
        val blockchain = getBlockchain(domainToken.networkId)

        return UserTokensResponse.Token(
            id = getResponseTokenId(domainToken),
            networkId = blockchain.toNetworkId(),
            derivationPath = domainToken.derivationPath,
            name = domainToken.name,
            symbol = domainToken.symbol,
            decimals = domainToken.decimals,
            contractAddress = domainToken.contractAddress,
        )
    }
}
