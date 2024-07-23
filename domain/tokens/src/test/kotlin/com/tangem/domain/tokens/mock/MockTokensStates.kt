package com.tangem.domain.tokens.mock

import arrow.core.nonEmptyListOf
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.tokens.model.CryptoCurrencyAmountStatus
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkAddress
import com.tangem.domain.tokens.model.NetworkStatus
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokensStates {

    val tokenState1 = CryptoCurrencyStatus(
        currency = MockTokens.token1,
        value = CryptoCurrencyStatus.Unreachable(
            priceChange = MockQuotes.quote1.priceChange,
            fiatRate = MockQuotes.quote1.fiatRate,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState2 = CryptoCurrencyStatus(
        currency = MockTokens.token2,
        value = CryptoCurrencyStatus.Unreachable(
            priceChange = MockQuotes.quote2.priceChange,
            fiatRate = MockQuotes.quote2.fiatRate,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState3 = CryptoCurrencyStatus(
        currency = MockTokens.token3,
        value = CryptoCurrencyStatus.Unreachable(
            priceChange = MockQuotes.quote3.priceChange,
            fiatRate = MockQuotes.quote3.fiatRate,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState4 = CryptoCurrencyStatus(
        currency = MockTokens.token4,
        value = CryptoCurrencyStatus.MissedDerivation(
            priceChange = MockQuotes.quote4.priceChange,
            fiatRate = MockQuotes.quote4.fiatRate,
        ),
    )

    val tokenState5 = CryptoCurrencyStatus(
        currency = MockTokens.token5,
        value = CryptoCurrencyStatus.MissedDerivation(
            priceChange = MockQuotes.quote5.priceChange,
            fiatRate = MockQuotes.quote5.fiatRate,
        ),
    )

    val tokenState6 = CryptoCurrencyStatus(
        currency = MockTokens.token6,
        value = CryptoCurrencyStatus.MissedDerivation(
            priceChange = MockQuotes.quote6.priceChange,
            fiatRate = MockQuotes.quote6.fiatRate,
        ),
    )

    val tokenState7 = CryptoCurrencyStatus(
        currency = MockTokens.token7,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote7.priceChange,
            fiatRate = MockQuotes.quote7.fiatRate,
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState8 = CryptoCurrencyStatus(
        currency = MockTokens.token8,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote8.priceChange,
            fiatRate = MockQuotes.quote8.fiatRate,
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState9 = CryptoCurrencyStatus(
        currency = MockTokens.token9,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote9.priceChange,
            fiatRate = MockQuotes.quote9.fiatRate,
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState10 = CryptoCurrencyStatus(
        currency = MockTokens.token10,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote10.priceChange,
            fiatRate = MockQuotes.quote10.fiatRate,
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
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
            .first { it.network == status.currency.network }
        val amount = (
            (networkStatus.value as NetworkStatus.Verified).amounts[status.currency.id]!!
                as? CryptoCurrencyAmountStatus.Loaded
            )?.value ?: BigDecimal.ZERO
        val quote = MockQuotes.quotes.first { it.rawCurrencyId == status.currency.id.rawCurrencyId }
        val fiatAmount = amount * quote.fiatRate

        status.copy(
            value = CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = fiatAmount,
                fiatRate = quote.fiatRate,
                priceChange = quote.priceChange,
                pendingTransactions = emptySet(),
                hasCurrentNetworkTransactions = false,
                networkAddress = requireNotNull(networkStatus.value as? NetworkStatus.Verified).address,
                yieldBalance = YieldBalance.Error,
            ),
        )
    }

    val noQuotesTokensStatuses = loadedTokensStates.map { status ->
        status.copy(
            value = CryptoCurrencyStatus.NoQuote(
                amount = status.value.amount!!,
                pendingTransactions = emptySet(),
                hasCurrentNetworkTransactions = false,
                networkAddress = requireNotNull(
                    value = MockNetworks.verifiedNetworksStatuses
                        .first { it.network == status.currency.network }
                        .value as? NetworkStatus.Verified,
                ).address,
                yieldBalance = YieldBalance.Error,
            ),
        )
    }
}
