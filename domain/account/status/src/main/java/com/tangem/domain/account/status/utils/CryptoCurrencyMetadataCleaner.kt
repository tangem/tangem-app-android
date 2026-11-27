package com.tangem.domain.account.status.utils

import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Class to clean up all data related to a specific cryptocurrency in a user's wallet.
 *
 * Each feature owns its removal logic and registers it as a [CryptoCurrencyDataCleaner].
 *
 * @property currencyDataCleaners Cleaners registered by features that keep per-currency data.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
 * @see <a href="https://www.notion.so/tangem/2be5d34eb67880008f95cb779dcafac9">Notion</a>
 *
[REDACTED_AUTHOR]
 */
class CryptoCurrencyMetadataCleaner(
    private val currencyDataCleaners: Set<CryptoCurrencyDataCleaner>,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletDataCleaner {

    /**
     * Removes all currency metadata for the given wallets.
     *
     * Invoked on wallet deletion, where the concrete currencies are no longer available, so cleanup happens by
     * wallet id in bulk instead of per-currency.
     *
     * @param userWalletIds The IDs of the deleted user wallets.
     */
    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        if (userWalletIds.isEmpty()) return

        clearAll { it.clear(userWalletIds) }
    }

    /**
     * Cleans up data for a single cryptocurrency in the specified user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currency The cryptocurrency to be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency) {
        invoke(userWalletId = userWalletId, currencies = listOf(currency))
    }

    /**
     * Cleans up data for multiple cryptocurrencies in the specified user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param currencies The list of cryptocurrencies to be cleaned.
     */
    suspend operator fun invoke(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return

        clearAll { it(userWalletId = userWalletId, currencies = currencies) }
    }

    private suspend fun clearAll(clear: suspend (CryptoCurrencyDataCleaner) -> Unit) {
        withContext(dispatchers.default) {
            currencyDataCleaners
                .map { cleaner ->
                    async { clearSafely(target = cleaner.javaClass.simpleName) { clear(cleaner) } }
                }
                .awaitAll()
        }
    }

    // Best-effort: isolate failures so a failing cleaner does not cancel the others.
    private suspend fun clearSafely(target: String, clear: suspend () -> Unit) {
        runSuspendCatching { clear() }
            .onFailure { TangemLogger.e("Failed to clear $target metadata", it) }
    }
}