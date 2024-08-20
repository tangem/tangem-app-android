package com.tangem.data.common.currency

import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.tokens.model.CryptoCurrency

class UserTokensResponseFactory {

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

    fun createResponseToken(currency: CryptoCurrency): UserTokensResponse.Token {
        val blockchain = getBlockchain(currency.network.id)

        return UserTokensResponse.Token(
            id = currency.id.rawCurrencyId,
            networkId = blockchain.toNetworkId(),
            derivationPath = currency.network.derivationPath.value,
            name = currency.name,
            symbol = currency.symbol,
            decimals = currency.decimals,
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
        )
    }
}