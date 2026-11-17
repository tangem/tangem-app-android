package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import kotlinx.coroutines.*

/**
 * Shared utility for fetching cryptocurrency balance data from multiple sources.
 *
 * This class provides reusable fetch operations for networks, quotes, and registered balance contributions
 * (staking and others) that can be used by different balance fetchers throughout the application.
 *
 * @property multiNetworkStatusFetcher Fetcher for updating network statuses.
 * @property multiQuoteStatusFetcher Fetcher for updating quote statuses.
 * @property contributionFetchers Fetchers of extra balances registered by their owning features.
 *
[REDACTED_AUTHOR]
 */
class BalanceFetchingOperations(
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val contributionFetchers: Set<BalanceContributionFetcher>,
) {

    /**
     * Fetches balance data from specified sources in parallel.
     *
     * @param userWalletId the user wallet identifier
     * @param currencies the list of cryptocurrencies to fetch data for
     * @param sources the set of sources to fetch from
     * @return map of errors (source name to throwable), empty if all succeeded. Contribution errors are keyed by
     * the fetcher's class simple name.
     */
    suspend fun fetchAll(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
        sources: Set<FetchingSource>,
    ): Map<String, Throwable> {
        return coroutineScope {
            sources
                .flatMap { source -> fetch(source, userWalletId, currencies) }
                .awaitAll()
                .mapNotNull { (name, result) ->
                    result.leftOrNull()?.let { error -> name to error }
                }
                .toMap()
        }
    }

    private fun CoroutineScope.fetch(
        source: FetchingSource,
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
    ): List<Deferred<Pair<String, Either<Throwable, Unit>>>> {
        return when (source) {
            FetchingSource.NETWORK -> listOf(
                async { source.name to fetchNetworks(userWalletId, currencies) },
            )
            FetchingSource.QUOTE -> listOf(
                async {
                    source.name to fetchQuotes(currencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId })
                },
            )
            FetchingSource.CONTRIBUTIONS -> contributionFetchers.map { fetcher ->
                async {
                    fetcher.javaClass.simpleName to fetcher.fetch(
                        userWalletId = userWalletId,
                        currencies = currencies,
                    )
                }
            }
        }
    }

    /**
     * Fetches network statuses for the given currencies.
     *
     * @param userWalletId the user wallet identifier
     * @param currencies the cryptocurrencies to fetch network statuses for
     * @return Either with Unit on success or Throwable on failure
     */
    suspend fun fetchNetworks(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        return multiNetworkStatusFetcher(
            params = MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = currencies.mapTo(hashSetOf(), CryptoCurrency::network),
            ),
        )
    }

    /**
     * Fetches quotes for the given raw currency ids.
     *
     * @param rawCurrencyIds the raw currency ids to fetch quotes for
     * @return Either with Unit on success or Throwable on failure
     */
    suspend fun fetchQuotes(rawCurrencyIds: Set<CryptoCurrency.RawID>): Either<Throwable, Unit> {
        return multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(currenciesIds = rawCurrencyIds, appCurrencyId = null),
        )
    }
}