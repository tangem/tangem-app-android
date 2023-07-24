package com.tangem.domain.tokens.mock

import arrow.core.nonEmptySetOf
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.model.TokenStatus

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokensStates {

    val tokenState1 = TokenStatus(
        id = MockTokens.token1.id,
        networkId = MockTokens.token1.networkId,
        name = MockTokens.token1.name,
        symbol = MockTokens.token1.symbol,
        decimals = MockTokens.token1.decimals,
        iconUrl = MockTokens.token1.iconUrl,
        isCoin = MockTokens.token1.isCoin,
        value = TokenStatus.Unreachable,
    )

    val tokenState2 = TokenStatus(
        id = MockTokens.token2.id,
        networkId = MockTokens.token2.networkId,
        name = MockTokens.token2.name,
        symbol = MockTokens.token2.symbol,
        decimals = MockTokens.token2.decimals,
        iconUrl = MockTokens.token2.iconUrl,
        isCoin = MockTokens.token2.isCoin,
        value = TokenStatus.Unreachable,
    )

    val tokenState3 = TokenStatus(
        id = MockTokens.token3.id,
        networkId = MockTokens.token3.networkId,
        name = MockTokens.token3.name,
        symbol = MockTokens.token3.symbol,
        decimals = MockTokens.token3.decimals,
        iconUrl = MockTokens.token3.iconUrl,
        isCoin = MockTokens.token3.isCoin,
        value = TokenStatus.Unreachable,
    )

    val tokenState4 = TokenStatus(
        id = MockTokens.token4.id,
        networkId = MockTokens.token4.networkId,
        name = MockTokens.token4.name,
        symbol = MockTokens.token4.symbol,
        decimals = MockTokens.token4.decimals,
        iconUrl = MockTokens.token4.iconUrl,
        isCoin = MockTokens.token4.isCoin,
        value = TokenStatus.MissedDerivation,
    )

    val tokenState5 = TokenStatus(
        id = MockTokens.token5.id,
        networkId = MockTokens.token5.networkId,
        name = MockTokens.token5.name,
        symbol = MockTokens.token5.symbol,
        decimals = MockTokens.token5.decimals,
        iconUrl = MockTokens.token5.iconUrl,
        isCoin = MockTokens.token5.isCoin,
        value = TokenStatus.MissedDerivation,
    )

    val tokenState6 = TokenStatus(
        id = MockTokens.token6.id,
        networkId = MockTokens.token6.networkId,
        name = MockTokens.token6.name,
        symbol = MockTokens.token6.symbol,
        decimals = MockTokens.token6.decimals,
        iconUrl = MockTokens.token6.iconUrl,
        isCoin = MockTokens.token6.isCoin,
        value = TokenStatus.MissedDerivation,
    )

    val tokenState7 = TokenStatus(
        id = MockTokens.token7.id,
        networkId = MockTokens.token7.networkId,
        name = MockTokens.token7.name,
        symbol = MockTokens.token7.symbol,
        decimals = MockTokens.token7.decimals,
        iconUrl = MockTokens.token7.iconUrl,
        isCoin = MockTokens.token7.isCoin,
        value = TokenStatus.NoAccount,
    )

    val tokenState8 = TokenStatus(
        id = MockTokens.token8.id,
        networkId = MockTokens.token8.networkId,
        name = MockTokens.token8.name,
        symbol = MockTokens.token8.symbol,
        decimals = MockTokens.token8.decimals,
        iconUrl = MockTokens.token8.iconUrl,
        isCoin = MockTokens.token8.isCoin,
        value = TokenStatus.NoAccount,
    )

    val tokenState9 = TokenStatus(
        id = MockTokens.token9.id,
        networkId = MockTokens.token9.networkId,
        name = MockTokens.token9.name,
        symbol = MockTokens.token9.symbol,
        decimals = MockTokens.token9.decimals,
        iconUrl = MockTokens.token9.iconUrl,
        isCoin = MockTokens.token9.isCoin,
        value = TokenStatus.NoAccount,
    )

    val tokenState10 = TokenStatus(
        id = MockTokens.token10.id,
        networkId = MockTokens.token10.networkId,
        name = MockTokens.token10.name,
        symbol = MockTokens.token10.symbol,
        decimals = MockTokens.token10.decimals,
        iconUrl = MockTokens.token10.iconUrl,
        isCoin = MockTokens.token10.isCoin,
        value = TokenStatus.NoAccount,
    )

    val tokenStates = nonEmptySetOf(
        tokenState1,
        tokenState2,
        tokenState3,
        tokenState4,
        tokenState5,
        tokenState6,
        tokenState7,
        tokenState8,
        tokenState9,
        tokenState10,
    )

    val loadedTokensStates = tokenStates.map { state ->
        val networkStatus = MockNetworks.verifiedNetworksStatuses.first { it.networkId == state.networkId }
        val amount = (networkStatus.value as NetworkStatus.Verified).amounts[state.id]!!
        val quote = MockQuotes.quotes.first { it.tokenId == state.id }
        val fiatAmount = amount * quote.fiatRate

        state.copy(
            value = TokenStatus.Loaded(
                amount = amount,
                fiatAmount = fiatAmount,
                priceChange = quote.priceChange,
                hasTransactionsInProgress = false,
            ),
        )
    }
}