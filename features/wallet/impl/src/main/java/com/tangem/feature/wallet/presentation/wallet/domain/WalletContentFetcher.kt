package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.account.fetcher.SingleAccountListFetcher
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wallet content fetcher
 *
 * @property walletBalanceFetcher fetcher for wallet balance
 * @property dispatchers          dispatchers
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class WalletContentFetcher @Inject constructor(
    private val walletBalanceFetcher: WalletBalanceFetcher,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val singleAccountListFetcher: SingleAccountListFetcher,
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletDataCleaner {

    private val fetchingJobMap = ConcurrentHashMap<UserWalletId, JobHolder>()
    private val lockedWalletFetchingJobMap = ConcurrentHashMap<UserWalletId, JobHolder>()
    private val mutex = Mutex()

    suspend operator fun invoke(userWalletId: UserWalletId, forceUpdate: Boolean = false) = supervisorScope {
        withContext(dispatchers.default) {
            val userWallet = userWalletsListRepository.getSyncOrNull(id = userWalletId)

            // Use mutex to ensure thread safety
            val fetchingJob: Job = mutex.withLock {
                if (userWallet?.isLocked == true) {
                    if (lockedWalletFetchingJobMap[userWalletId]?.isActive == true) return@withContext

                    return@withLock launch { fetchLockedWalletAccounts(userWallet) }
                        .saveIn(JobHolder().also { lockedWalletFetchingJobMap[userWalletId] = it })
                }
                val savedJobHolder = fetchingJobMap[userWalletId]

                // If this is not a forced update and the balance is already being fetched or has been fetched,
                // then skip the update process.
                if (!forceUpdate && savedJobHolder.isFetchingStartedOrFinished()) {
                    TangemLogger.d("Skip fetching for $userWalletId")

                    return@withContext
                }

                /*
                 * If this is a forced update and there is already a job in the cache that is updating the balance,
                 * then cancel the previous update.
                 */
                if (forceUpdate && savedJobHolder?.isActive == true) {
                    TangemLogger.d("Cancel old fetching for $userWalletId")

                    savedJobHolder.cancel()
                }

                TangemLogger.d("Start fetching for $userWalletId")

                /*
                 * The job is saved while the lock is still held: otherwise [clear] could slip in between launching
                 * and saving the job, and then the fetch of an already deleted wallet would keep running.
                 */
                launch {
                    walletBalanceFetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))
                        .onLeft { TangemLogger.e("Error", it) }
                }
                    .saveIn(JobHolder().also { fetchingJobMap[userWalletId] = it })
            }

            fetchingJob.join()

            TangemLogger.d("Finish fetching for $userWalletId")
        }
    }

    /**
     * The content of a locked wallet can't be fetched, but the account lists of all wallets are combined, so a wallet
     * whose accounts were never saved would keep the other wallets loading forever.
     */
    private suspend fun fetchLockedWalletAccounts(userWallet: UserWallet) {
        if (!userWallet.isMultiCurrency) return

        val userWalletId = userWallet.walletId
        if (accountsCRUDRepository.getAccountListSync(userWalletId).isSome()) return

        singleAccountListFetcher(params = SingleAccountListFetcher.Params(userWalletId))
            .onLeft { TangemLogger.e("Failed to fetch accounts of locked wallet $userWalletId", it) }
    }

    /**
     * Drops the cached fetching state of the deleted wallets.
     *
     * A re-added wallet reuses the same [UserWalletId], so a completed job left in the cache would make the fetcher
     * skip loading and the wallet screen would hang on infinite loading.
     *
     * @param userWalletIds ids of the deleted wallets
     */
    override suspend fun clear(userWalletIds: List<UserWalletId>) {
        mutex.withLock {
            userWalletIds.forEach { userWalletId ->
                fetchingJobMap.remove(userWalletId)?.cancel()
                lockedWalletFetchingJobMap.remove(userWalletId)?.cancel()

                TangemLogger.d("Clear fetching state for $userWalletId")
            }
        }
    }

    /**
     * Whether a fetch has already been started for this wallet, no matter if it is still running or has finished.
     *
     * A cancelled job doesn't count: the content stayed incomplete, so leaving the wallet screen mid-fetch must not
     * block the next attempt.
     */
    private fun JobHolder?.isFetchingStartedOrFinished(): Boolean {
        return this != null && !isEmpty() && !isCancelled
    }
}