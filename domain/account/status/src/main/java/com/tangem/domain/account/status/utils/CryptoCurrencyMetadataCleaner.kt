package com.tangem.domain.account.status.utils

import com.tangem.domain.common.wallets.CryptoCurrencyDataCleaner
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.utils.NetworksCleaner
import com.tangem.domain.nft.utils.NFTCleaner
import com.tangem.domain.staking.utils.StakingCleaner
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.utils.annotations.RemoveWithToggle
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
 * @property tokensFeatureToggles Toggles switching between the registered cleaners and the legacy hard-wired ones.
 * @property networksCleaner Utility to clean network-related data.
 * @property stakingCleaner Utility to clean staking-related data.
 * @property nftCleaner Utility to clean NFT-related data.
 * @property dispatchers Coroutine dispatchers for managing threading.
 *
 * @see <a href="https://www.notion.so/tangem/2be5d34eb67880008f95cb779dcafac9">Notion</a>
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class CryptoCurrencyMetadataCleaner(
    private val currencyDataCleaners: Set<CryptoCurrencyDataCleaner>,
    private val tokensFeatureToggles: TokensFeatureToggles,
    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private val networksCleaner: NetworksCleaner,
    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private val stakingCleaner: StakingCleaner,
    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private val nftCleaner: NFTCleaner,
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

        if (!tokensFeatureToggles.isPluggableBalanceSourcesEnabled) {
            clearLegacy(userWalletIds)
            return
        }

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

        if (!tokensFeatureToggles.isPluggableBalanceSourcesEnabled) {
            invokeLegacy(userWalletId = userWalletId, currencies = currencies)
            return
        }

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

    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private suspend fun clearLegacy(userWalletIds: List<UserWalletId>) {
        withContext(dispatchers.default) {
            awaitAll(
                async { clearSafely(target = "networks") { networksCleaner.clear(userWalletIds) } },
                async { clearSafely(target = "staking") { stakingCleaner.clear(userWalletIds) } },
            )
        }
    }

    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private suspend fun invokeLegacy(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        withContext(dispatchers.default) {
            awaitAll(
                async { networksCleaner(userWalletId = userWalletId, currencies = currencies) },
                async { clearStaking(userWalletId = userWalletId, currencies = currencies) },
                async { clearNFTs(userWalletId = userWalletId, currencies = currencies) },
            )
        }
    }

    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private suspend fun clearStaking(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        stakingCleaner(userWalletId = userWalletId, currencies = currencies)
    }

    @RemoveWithToggle(toggleName = "AND_18965_PLUGGABLE_BALANCE_SOURCES_ENABLED")
    private suspend fun clearNFTs(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        val networks = currencies.mapTo(destination = hashSetOf(), transform = CryptoCurrency::network)

        nftCleaner(userWalletId = userWalletId, networks = networks)
    }
}