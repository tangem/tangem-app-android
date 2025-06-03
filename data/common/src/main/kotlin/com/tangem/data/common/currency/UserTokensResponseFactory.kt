package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.currency.CryptoCurrency

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
        return with(currency) {
            UserTokensResponse.Token(
                id = id.rawCurrencyId?.value,
                networkId = network.backendId,
                derivationPath = network.derivationPath.value,
                name = name,
                symbol = symbol,
                decimals = decimals,
                contractAddress = (this as? CryptoCurrency.Token)?.contractAddress,
            )
        }
    }
}