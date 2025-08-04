package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.tokens.wallet.WalletBalanceFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveInAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
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
) {

    private val fetchingJobMap = ConcurrentHashMap<UserWalletId, JobHolder>()
    private val mutex = Mutex()

    suspend operator fun invoke(userWalletId: UserWalletId, forceUpdate: Boolean = false) = supervisorScope {
        withContext(dispatchers.default) {
            // Use mutex to ensure thread safety
            val jobHolder = mutex.withLock {
                val savedJobHolder = fetchingJobMap.getOrPut(key = userWalletId, defaultValue = ::JobHolder)

                /*
                 * If this is not a forced update and there is a saved job in the cache
                 * (doesn't matter if it is active or not), then skip the update process.
                 */
                if (!forceUpdate && savedJobHolder != null && !savedJobHolder.isEmpty()) {
                    Timber.d("Skip fetching for $userWalletId")

                    return@withContext
                }

                /*
                 * If this is a forced update and there is already a job in the cache that is updating the balance,
                 * then cancel the previous update.
                 */
                if (forceUpdate && savedJobHolder?.isActive == true) {
                    Timber.d("Cancel old fetching for $userWalletId")

                    savedJobHolder.cancel()
                }

                JobHolder().also { fetchingJobMap[userWalletId] = it }
            }

            Timber.d("Start fetching for $userWalletId")

            val maybeResult = launch {
                walletBalanceFetcher(params = WalletBalanceFetcher.Params(userWalletId = userWalletId))
                    .onLeft(Timber::e)
            }
                .saveInAndJoin(jobHolder)

            Timber.d("Finish fetching with result $maybeResult for $userWalletId")
        }
    }
}