package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.right
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

/**
 * Shared utility for fetching cryptocurrency balance data from multiple sources.
 *
 * This class provides reusable fetch operations for networks, quotes, and staking balances
 * that can be used by different balance fetchers throughout the application.
 *
 * @property multiNetworkStatusFetcher Fetcher for updating network statuses.
 * @property multiQuoteStatusFetcher Fetcher for updating quote statuses.
 * @property multiStakingBalanceFetcher Fetcher for updating staking balances.
 * @property stakingIdFactory Factory for creating staking IDs.
 *
[REDACTED_AUTHOR]
 */
class BalanceFetchingOperations(
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
) {

    /**
     * Fetches balance data from specified sources in parallel.
     *
     * @param userWalletId the user wallet identifier
     * @param currencies the list of cryptocurrencies to fetch data for
     * @param sources the set of sources to fetch from
     * @return map of errors (source to throwable), empty if all succeeded
     */
    suspend fun fetchAll(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
        sources: Set<FetchingSource>,
    ): Map<FetchingSource, Throwable> {
        return coroutineScope {
            sources.map { source ->
                async {
                    val result = when (source) {
                        FetchingSource.NETWORK -> fetchNetworks(userWalletId, currencies)
                        FetchingSource.QUOTE -> fetchQuotes(currencies)
                        FetchingSource.STAKING -> fetchStaking(userWalletId, currencies)
                    }
                    source to result
                }
            }
                .awaitAll()
                .mapNotNull { (source, result) ->
                    result.leftOrNull()?.let { error -> source to error }
                }
                .toMap()
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
     * Fetches quotes for the given currencies.
     *
     * @param currencies the cryptocurrencies to fetch quotes for
     * @return Either with Unit on success or Throwable on failure
     */
    suspend fun fetchQuotes(currencies: Collection<CryptoCurrency>): Either<Throwable, Unit> {
        return multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(
                currenciesIds = currencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                appCurrencyId = null,
            ),
        )
    }

    /**
     * Fetches staking balances for the given currencies.
     *
     * Logs errors for currencies where staking ID cannot be obtained.
     *
     * @param userWalletId the user wallet identifier
     * @param currencies the cryptocurrencies to fetch staking balances for
     * @return Either with Unit on success or Throwable on failure
     */
    suspend fun fetchStaking(
        userWalletId: UserWalletId,
        currencies: Collection<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        val stakingIds = currencies.mapNotNullTo(hashSetOf()) { currency ->
            val stakingId = stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = currency)

            if (stakingId.isLeft { it is StakingIdFactory.Error.UnableToGetAddress }) {
                Timber.e("Unable to get staking ID for user wallet $userWalletId and currency ${currency.id}")
            }

            stakingId.getOrNull()
        }

        if (stakingIds.isEmpty()) {
            Timber.i("No staking IDs found for user wallet $userWalletId")
            return Unit.right()
        }

        return multiStakingBalanceFetcher(
            params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
        )
    }
}