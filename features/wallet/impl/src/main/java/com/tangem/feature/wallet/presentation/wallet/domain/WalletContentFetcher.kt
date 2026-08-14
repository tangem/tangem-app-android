package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.common.wallets.UserWalletDataCleaner
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
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
    private val dispatchers: CoroutineDispatcherProvider,
) : UserWalletDataCleaner {

    private val fetchingJobMap = ConcurrentHashMap<UserWalletId, JobHolder>()
    private val mutex = Mutex()

    suspend operator fun invoke(userWalletId: UserWalletId, forceUpdate: Boolean = false) = supervisorScope {
        withContext(dispatchers.default) {
            // Use mutex to ensure thread safety
            val fetchingJob = mutex.withLock {
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