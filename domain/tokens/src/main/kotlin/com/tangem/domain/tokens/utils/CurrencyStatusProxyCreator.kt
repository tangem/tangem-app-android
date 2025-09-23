package com.tangem.domain.tokens.utils

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.recover
import arrow.core.toNonEmptySetOrNull
import arrow.core.toOption
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.getAddress
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.staking.YieldBalance
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.domain.tokens.operations.CryptoCurrencyStatusFactory
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations.Error

/**
 * Proxy creator of [CryptoCurrencyStatus]. Used [CryptoCurrencyStatusFactory] to create statuses.
 *
[REDACTED_AUTHOR]
 */
class CurrencyStatusProxyCreator {

    fun createCurrencyStatus(
        currency: CryptoCurrency,
        maybeQuoteStatus: Either<Error, QuoteStatus?>,
        maybeNetworkStatus: Either<Error, NetworkStatus?>,
        maybeYieldBalance: Either<Error, YieldBalance>?,
    ): Either<Error, CryptoCurrencyStatus> = either {
        val networkStatus = maybeNetworkStatus.bind()
        val quote = recover(
            block = { maybeQuoteStatus.bind() },
            recover = { null },
        )
        val yieldBalance = maybeYieldBalance?.getOrNull()

        createCurrencyStatus(
            currency = currency,
            quoteStatus = quote,
            networkStatus = networkStatus,
            yieldBalance = yieldBalance,
        )
    }

    fun createCurrenciesStatuses(
        currencies: NonEmptyList<CryptoCurrency>,
        maybeQuotes: Either<Error, Set<QuoteStatus>>?,
        maybeNetworkStatuses: Either<Error, Set<NetworkStatus>>,
        maybeYieldBalances: Either<Error, List<YieldBalance>>,
    ): Either<Error, List<CryptoCurrencyStatus>> = either {
        val networksStatuses = maybeNetworkStatuses.bind().toNonEmptySetOrNull()
        val quoteStatuses: Set<QuoteStatus>? = maybeQuotes?.getOrNull()?.ifEmpty { null }

        val yieldBalances = maybeYieldBalances.getOrNull()

        currencies.map { currency ->
            val quote = quoteStatuses?.firstOrNull { it.rawCurrencyId == currency.id.rawCurrencyId }
            val networkStatus = networksStatuses?.firstOrNull { it.network == currency.network }
            val address = networkStatus.getAddress()

            val supportedIntegration = StakingIntegrationID.create(currencyId = currency.id)?.value

            val yieldBalance = if (supportedIntegration != null && address != null) {
                val stakingId = StakingID(integrationId = supportedIntegration, address = address)

                yieldBalances?.firstOrNull { it.stakingId == stakingId }
                    ?: YieldBalance.Error(stakingId = stakingId)
            } else {
                null
            }

            createCurrencyStatus(
                currency = currency,
                quoteStatus = quote,
                networkStatus = networkStatus,
                yieldBalance = yieldBalance,
            )
        }
    }

    private fun createCurrencyStatus(
        currency: CryptoCurrency,
        quoteStatus: QuoteStatus?,
        networkStatus: NetworkStatus?,
        yieldBalance: YieldBalance?,
    ): CryptoCurrencyStatus {
        return CryptoCurrencyStatusFactory.create(
            currency = currency,
            maybeNetworkStatus = networkStatus.toOption(),
            maybeQuoteStatus = quoteStatus.toOption(),
            maybeYieldBalance = yieldBalance.toOption(),
        )
    }
}