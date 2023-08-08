package com.tangem.domain.tokens.mock

import arrow.core.nonEmptyListOf
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkStatus

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokensStates {

    val tokenState1 = CryptoCurrencyStatus(
        currency = MockTokens.token1,
        value = CryptoCurrencyStatus.Unreachable,
    )

    val tokenState2 = CryptoCurrencyStatus(
        currency = MockTokens.token2,
        value = CryptoCurrencyStatus.Unreachable,
    )

    val tokenState3 = CryptoCurrencyStatus(
        currency = MockTokens.token3,
        value = CryptoCurrencyStatus.Unreachable,
    )

    val tokenState4 = CryptoCurrencyStatus(
        currency = MockTokens.token4,
        value = CryptoCurrencyStatus.MissedDerivation,
    )

    val tokenState5 = CryptoCurrencyStatus(
        currency = MockTokens.token5,
        value = CryptoCurrencyStatus.MissedDerivation,
    )

    val tokenState6 = CryptoCurrencyStatus(
        currency = MockTokens.token6,
        value = CryptoCurrencyStatus.MissedDerivation,
    )

    val tokenState7 = CryptoCurrencyStatus(
        currency = MockTokens.token7,
        value = CryptoCurrencyStatus.NoAccount,
    )

    val tokenState8 = CryptoCurrencyStatus(
        currency = MockTokens.token8,
        value = CryptoCurrencyStatus.NoAccount,
    )

    val tokenState9 = CryptoCurrencyStatus(
        currency = MockTokens.token9,
        value = CryptoCurrencyStatus.NoAccount,
    )

    val tokenState10 = CryptoCurrencyStatus(
        currency = MockTokens.token10,
        value = CryptoCurrencyStatus.NoAccount,
    )

    val failedTokenStates = nonEmptyListOf(
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

    val loadedTokensStates = failedTokenStates.map { status ->
        val networkStatus = MockNetworks.verifiedNetworksStatuses
            .first { it.networkId == status.currency.networkId }
        val amount = (networkStatus.value as NetworkStatus.Verified).amounts[status.currency.id]!!
        val quote = MockQuotes.quotes.first { it.currencyId == status.currency.id }
        val fiatAmount = amount * quote.fiatRate

        status.copy(
            value = CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = fiatAmount,
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                hasTransactionsInProgress = false,
            ),
        )
    }
}