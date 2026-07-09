package com.tangem.domain.account.status.utils

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Class to clean up all data related to a specific cryptocurrency in a user's wallet.
 *
 * @property networksCleaner Utility to clean network-related data.
 * @property stakingCleaner Utility to clean staking-related data.
 * @property nftCleaner Utility to clean NFT-related data.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
 * @see <a href="https://www.notion.so/tangem/2be5d34eb67880008f95cb779dcafac9">Notion</a>
 *
[REDACTED_AUTHOR]
 */
class CryptoCurrencyMetadataCleaner(
    private val networksCleaner: NetworksCleaner,
    private val stakingCleaner: StakingCleaner,
    private val nftCleaner: NFTCleaner,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletDataCleaner {

    /**
     * Removes all currency metadata (networks, staking balances) for the given wallets.
     *
     * Invoked on wallet deletion, where the concrete currencies are no longer available, so cleanup happens by
     * wallet id in bulk instead of per-currency.
     *
     * @param userWalletIds The IDs of the deleted user wallets.
     */
    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        if (userWalletIds.isEmpty()) return

        // Best-effort: isolate failures so a failing cleaner does not cancel the other.
        withContext(dispatchers.default) {
            awaitAll(
                async { clearSafely(target = "networks") { networksCleaner.clear(userWalletIds) } },
                async { clearSafely(target = "staking") { stakingCleaner.clear(userWalletIds) } },
            )
        }
    }

    private suspend fun clearSafely(target: String, clear: suspend () -> Unit) {
        runSuspendCatching { clear() }
            .onFailure { TangemLogger.e("Failed to clear $target metadata", it) }
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

        return withContext(dispatchers.default) {
            awaitAll(
                async { networksCleaner(userWalletId = userWalletId, currencies = currencies) },
                async { clearStaking(userWalletId = userWalletId, currencies = currencies) },
                async { clearNFTs(userWalletId = userWalletId, currencies = currencies) },
            )
        }
    }

    private suspend fun clearStaking(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        stakingCleaner(userWalletId = userWalletId, currencies = currencies)
    }

    private suspend fun clearNFTs(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network)

        nftCleaner(userWalletId = userWalletId, networks = networks)
    }
}