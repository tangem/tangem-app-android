package com.tangem.domain.tokens.mock

import arrow.core.nonEmptyListOf
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.quote.fold
import java.math.BigDecimal

@Suppress("MemberVisibilityCanBePrivate")
internal object MockTokensStates {

    val tokenState1 = CryptoCurrencyStatus(
        currency = MockTokens.token1,
        value = CryptoCurrencyStatus.Unreachable(
            priceChange = MockQuotes.quote1.getPriceChange(),
            fiatRate = MockQuotes.quote1.getFiatRate(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState2 = CryptoCurrencyStatus(
        currency = MockTokens.token2,
        value = CryptoCurrencyStatus.Unreachable(
            priceChange = MockQuotes.quote2.getPriceChange(),
            fiatRate = MockQuotes.quote2.getFiatRate(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState3 = CryptoCurrencyStatus(
        currency = MockTokens.token3,
        value = CryptoCurrencyStatus.Unreachable(
            priceChange = MockQuotes.quote3.getPriceChange(),
            fiatRate = MockQuotes.quote3.getFiatRate(),
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
        ),
    )

    val tokenState4 = CryptoCurrencyStatus(
        currency = MockTokens.token4,
        value = CryptoCurrencyStatus.MissedDerivation(
            priceChange = MockQuotes.quote4.getPriceChange(),
            fiatRate = MockQuotes.quote4.getFiatRate(),
        ),
    )

    val tokenState5 = CryptoCurrencyStatus(
        currency = MockTokens.token5,
        value = CryptoCurrencyStatus.MissedDerivation(
            priceChange = MockQuotes.quote5.getPriceChange(),
            fiatRate = MockQuotes.quote5.getFiatRate(),
        ),
    )

    val tokenState6 = CryptoCurrencyStatus(
        currency = MockTokens.token6,
        value = CryptoCurrencyStatus.MissedDerivation(
            priceChange = MockQuotes.quote6.getPriceChange(),
            fiatRate = MockQuotes.quote6.getFiatRate(),
        ),
    )

    val tokenState7 = CryptoCurrencyStatus(
        currency = MockTokens.token7,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote7.getPriceChange(),
            fiatRate = MockQuotes.quote7.getFiatRate(),
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    val tokenState8 = CryptoCurrencyStatus(
        currency = MockTokens.token8,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote8.getPriceChange(),
            fiatRate = MockQuotes.quote8.getFiatRate(),
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    val tokenState9 = CryptoCurrencyStatus(
        currency = MockTokens.token9,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote9.getPriceChange(),
            fiatRate = MockQuotes.quote9.getFiatRate(),
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
        ),
    )

    val tokenState10 = CryptoCurrencyStatus(
        currency = MockTokens.token10,
        value = CryptoCurrencyStatus.NoAccount(
            fiatAmount = BigDecimal.ZERO,
            priceChange = MockQuotes.quote10.getPriceChange(),
            fiatRate = MockQuotes.quote10.getFiatRate(),
            amountToCreateAccount = MockNetworks.amountToCreateAccount,
            networkAddress = NetworkAddress.Single(
                defaultAddress = NetworkAddress.Address(value = "mock", NetworkAddress.Address.Type.Primary),
            ),
            sources = CryptoCurrencyStatus.Sources(),
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
                as? NetworkStatus.Amount.Loaded
            )?.value ?: BigDecimal.ZERO
        val quote = MockQuotes.quotes.first { it.rawCurrencyId == status.currency.id.rawCurrencyId }

        val value = when (val value = quote.value) {
            is QuoteStatus.Empty -> CryptoCurrencyStatus.NoQuote(
                amount = status.value.amount!!,
                pendingTransactions = emptySet(),
                hasCurrentNetworkTransactions = false,
                networkAddress = requireNotNull(
                    value = MockNetworks.verifiedNetworksStatuses.first { it.network == status.currency.network }.value as? NetworkStatus.Verified,
                ).address,
                yieldBalance = null,
                sources = CryptoCurrencyStatus.Sources(),
                yieldSupplyStatus = null,
            )
            is QuoteStatus.Data -> CryptoCurrencyStatus.Loaded(
                amount = amount,
                fiatAmount = amount * value.fiatRate,
                fiatRate = value.fiatRate,
                priceChange = value.priceChange,
                pendingTransactions = emptySet(),
                hasCurrentNetworkTransactions = false,
                networkAddress = requireNotNull(networkStatus.value as? NetworkStatus.Verified).address,
                yieldBalance = null,
                sources = CryptoCurrencyStatus.Sources(),
                yieldSupplyStatus = null,
            )
        }
        status.copy(value = value)
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
                yieldBalance = null,
                sources = CryptoCurrencyStatus.Sources(),
                yieldSupplyStatus = null,
            ),
        )
    }

    private fun QuoteStatus.getPriceChange(): BigDecimal? = fold(onData = { priceChange }, onEmpty = { null })
    private fun QuoteStatus.getFiatRate(): BigDecimal? = fold(onData = { fiatRate }, onEmpty = { null })
}