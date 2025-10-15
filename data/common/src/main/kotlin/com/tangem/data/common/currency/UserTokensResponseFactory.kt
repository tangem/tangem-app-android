package com.tangem.data.common.currency

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.currency.CryptoCurrency
import javax.inject.Inject

// TODO: [REDACTED_JIRA]
class UserTokensResponseFactory @Inject constructor() {

    fun createUserTokensResponse(
        currencies: List<CryptoCurrency>,
        isGroupedByNetwork: Boolean,
        isSortedByBalance: Boolean,
        accountId: AccountId? = null,
    ): UserTokensResponse {
        return UserTokensResponse(
            tokens = currencies.map { createResponseToken(currency = it, accountId = accountId) },
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

    fun createResponseToken(currency: CryptoCurrency, accountId: AccountId? = null): UserTokensResponse.Token {
        return with(currency) {
            UserTokensResponse.Token(
                id = id.rawCurrencyId?.value,
                accountId = accountId?.value,
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