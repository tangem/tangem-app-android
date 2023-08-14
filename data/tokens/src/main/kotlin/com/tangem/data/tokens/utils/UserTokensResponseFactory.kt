package com.tangem.data.tokens.utils

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.tokens.models.CryptoCurrency

internal class UserTokensResponseFactory {

    fun createUserTokensResponse(
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
    ): UserTokensResponse {
        return UserTokensResponse(
            tokens = currencies.map(::createResponseToken),
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

    private fun createResponseToken(currency: CryptoCurrency): UserTokensResponse.Token {
        val blockchain = getBlockchain(currency.networkId)

        return UserTokensResponse.Token(
            id = currency.id.rawCurrencyId,
            networkId = blockchain.toNetworkId(),
            derivationPath = currency.derivationPath,
            name = currency.name,
            symbol = currency.symbol,
            decimals = currency.decimals,
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
        )
    }
}
