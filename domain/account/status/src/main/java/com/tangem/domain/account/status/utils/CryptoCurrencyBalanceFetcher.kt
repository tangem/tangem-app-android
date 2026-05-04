package com.tangem.domain.account.status.utils

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.BalanceFetchingOperations
import com.tangem.domain.tokens.FetchErrorFormatter
import com.tangem.domain.tokens.FetchingSource
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class responsible for fetching and refreshing the balances of various crypto currencies
 * associated with a user's wallet.
 *
 * Uses [BalanceFetchingOperations] for the actual fetching logic.
 * Uses per-wallet mutex to allow concurrent refreshes for different wallets while preventing
 * concurrent refreshes for the same wallet.
 *
 * @property balanceFetchingOperations shared operations for fetching balance data
 * @property parallelUpdatingScope coroutine scope for parallel balance updates
 *
[REDACTED_AUTHOR]
 */
class CryptoCurrencyBalanceFetcher(
    private val balanceFetchingOperations: BalanceFetchingOperations,
    private val parallelUpdatingScope: CoroutineScope,
) {

    private val mutexMap = ConcurrentHashMap<UserWalletId, Mutex>()

    /**
     * Fire-and-forget balance refresh for a single currency.
     */
    operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency) {
        invoke(userWalletId = userWalletId, currencies = listOf(currency))
    }

    /**
     * Fire-and-forget balance refresh for multiple currencies.
     * Launches in [parallelUpdatingScope] and uses per-wallet mutex to prevent concurrent refreshes
     * for the same wallet while allowing parallel refreshes for different wallets.
     */
    operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        parallelUpdatingScope.launch {
            getMutex(userWalletId).withLock {
                refreshBalances(userWalletId, currencies)
            }
        }
    }

    /**
     * Suspending balance refresh for a single currency.
     * Awaits completion before returning.
     */
    suspend fun invokeAndAwait(userWalletId: UserWalletId, currency: CryptoCurrency) {
        invokeAndAwait(userWalletId = userWalletId, currencies = listOf(currency))
    }

    /**
     * Suspending balance refresh for multiple currencies.
     * Awaits completion before returning, uses per-wallet mutex to prevent concurrent refreshes
     * for the same wallet while allowing parallel refreshes for different wallets.
     */
    suspend fun invokeAndAwait(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        getMutex(userWalletId).withLock {
            refreshBalances(userWalletId, currencies)
        }
    }

    private fun getMutex(userWalletId: UserWalletId): Mutex {
        return mutexMap.computeIfAbsent(userWalletId) { Mutex() }
    }

    private suspend fun refreshBalances(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val errors = balanceFetchingOperations.fetchAll(
            userWalletId = userWalletId,
            currencies = currencies,
            sources = FETCHING_SOURCES,
        )

        if (errors.isNotEmpty()) {
            TangemLogger.e(FetchErrorFormatter.format(userWalletId, errors))
        }
    }

    private companion object {
        val FETCHING_SOURCES = setOf(
            FetchingSource.NETWORK,
            FetchingSource.QUOTE,
            FetchingSource.STAKING,
        )
    }
}