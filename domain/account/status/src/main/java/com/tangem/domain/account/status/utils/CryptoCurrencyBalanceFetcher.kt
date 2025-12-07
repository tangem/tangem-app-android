package com.tangem.domain.account.status.utils

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.tokens.wallet.FetchingSource
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Utility class responsible for fetching and refreshing the balances of various crypto currencies
 * associated with a user's wallet.
 *
 * @property multiNetworkStatusFetcher Fetcher for updating network statuses.
 * @property multiQuoteStatusFetcher Fetcher for updating quote statuses.
 * @property multiStakingBalanceFetcher Fetcher for updating staking balances.
 * @property stakingIdFactory Factory for creating staking IDs.
 * @property parallelUpdatingScope Coroutine scope for parallel balance updates.
 *
[REDACTED_AUTHOR]
 */
class CryptoCurrencyBalanceFetcher(
    private val multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
    private val multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
    private val multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
    private val stakingIdFactory: StakingIdFactory,
    private val parallelUpdatingScope: CoroutineScope,
) {

    private val mutex = Mutex()

    operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        parallelUpdatingScope.launch {
            mutex.withLock {
                refreshBalances(userWalletId, currencies)
            }
        }
    }

    private suspend fun refreshBalances(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        coroutineScope {
            val results = listOf(
                async {
                    FetchingSource.NETWORK to refreshNetworks(userWalletId = userWalletId, currencies = currencies)
                },
                async {
                    FetchingSource.STAKING to refreshStakingBalances(
                        userWalletId = userWalletId,
                        currencies = currencies,
                    )
                },
                async { FetchingSource.QUOTE to refreshQuotes(currencies = currencies) },
            )
                .awaitAll()

            val errors = results.mapNotNull { (source, maybeResult) ->
                val error = maybeResult.leftOrNull() ?: return@mapNotNull null

                source to error
            }

            if (errors.isNotEmpty()) {
                val message = "Failed to fetch next sources for $userWalletId:\n" +
                    errors.joinToString(separator = "\n") { "${it.first.name} – ${it.second}" }

                Timber.e(message)
            }
        }
    }

    private suspend fun refreshNetworks(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        return multiNetworkStatusFetcher(
            params = MultiNetworkStatusFetcher.Params(
                userWalletId = userWalletId,
                networks = currencies.mapTo(hashSetOf(), CryptoCurrency::network),
            ),
        )
    }

    private suspend fun refreshStakingBalances(
        userWalletId: UserWalletId,
        currencies: List<CryptoCurrency>,
    ): Either<Throwable, Unit> {
        val stakingIds = currencies.mapNotNullTo(hashSetOf()) {
            stakingIdFactory.create(userWalletId = userWalletId, cryptoCurrency = it).getOrNull()
        }

        return multiStakingBalanceFetcher(
            params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds),
        )
    }

    private suspend fun refreshQuotes(currencies: List<CryptoCurrency>): Either<Throwable, Unit> {
        return multiQuoteStatusFetcher(
            params = MultiQuoteStatusFetcher.Params(
                currenciesIds = currencies.mapNotNullTo(hashSetOf()) { it.id.rawCurrencyId },
                appCurrencyId = null,
            ),
        )
    }
}