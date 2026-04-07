package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker.Companion.CHECK_INTERVAL_MS
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Periodically checks pending Yield Supply transaction hashes for a given set of wallet/currency pairs.
 *
 * - Call [addPending] right after sending a transaction to start tracking its hashes.
 * - The checker runs every [CHECK_INTERVAL_MS] and queries repository for the current pending tx hashes.
 * - If none of the stored hashes are pending anymore for a tracked entry, it triggers a single-network status refresh
 *   via [singleNetworkStatusFetcher] and removes the entry from tracking.
 * - When [expectedActive] is provided, after txs are confirmed the tracker enters a post-confirmation
 *   verification phase: it checks the actual yield protocol status and retries up to [MAX_POST_CONFIRMATION_RETRIES]
 *   times before clearing the pending status.
 * - The periodic job stops automatically when there is nothing left to track.
 */
class YieldSupplyPendingTracker(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val coroutineScope: AppCoroutineScope,
) {

    private data class TrackedKey(
        val userWalletId: UserWalletId,
        val cryptoCurrencyId: CryptoCurrency.ID,
    )

    private data class TrackedEntry(
        val cryptoCurrency: CryptoCurrency,
        val txIds: Set<String>,
        val attempts: Int = 0,
        val expectedActive: Boolean? = null,
        val isInPostConfirmation: Boolean = false,
        val postConfirmationRetries: Int = 0,
    )

    private val trackedEntries = ConcurrentHashMap<TrackedKey, TrackedEntry>()
    private val mutex = Mutex()
    private var checkingJob: Job? = null

    /**
     * Add tx ids to track for a wallet/currency pair. Starts periodic checking if needed.
     *
     * @param expectedActive If non-null, after all txs are confirmed the tracker will verify
     *   that the yield protocol status matches this value, retrying up to 2 times at 10s intervals.
     *   Pass `true` for enter-yield, `false` for exit-yield, `null` to skip verification.
     */
    suspend fun addPending(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        txIds: List<String>,
        expectedActive: Boolean? = null,
    ) {
        val key = TrackedKey(userWalletId, cryptoCurrency.id)
        trackedEntries.compute(key) { _, existing ->
            if (existing == null) {
                TrackedEntry(
                    cryptoCurrency = cryptoCurrency,
                    txIds = txIds.toSet(),
                    expectedActive = expectedActive,
                )
            } else {
                existing.copy(
                    txIds = existing.txIds + txIds,
                    expectedActive = expectedActive ?: existing.expectedActive,
                )
            }
        }

        startAutomaticCheckingIfNeeded()
    }

    private suspend fun startAutomaticCheckingIfNeeded() {
        mutex.withLock {
            if (checkingJob?.isActive == true) return

            checkingJob = coroutineScope.launch {
                while (isActive) {
                    delay(CHECK_INTERVAL_MS)
                    try {
                        checkAllTracked()
                    } catch (ex: Exception) {
                        TangemLogger.e("Error", ex)
                    }
                }
            }
        }
    }

    private suspend fun stopAutomaticCheckingIfEmpty() {
        if (trackedEntries.isEmpty()) {
            mutex.withLock {
                if (trackedEntries.isEmpty()) {
                    checkingJob?.cancel()
                    checkingJob = null
                }
            }
        }
    }

    private suspend fun checkAllTracked() {
        val keysSnapshot = trackedEntries.keys().toList()

        if (keysSnapshot.isEmpty()) {
            stopAutomaticCheckingIfEmpty()
            return
        }

        val networksToFetch = mutableSetOf<SingleNetworkStatusFetcher.Params>()

        for (key in keysSnapshot) {
            processTrackedEntry(key, networksToFetch)
        }

        for (params in networksToFetch) {
            singleNetworkStatusFetcher(params)
        }

        stopAutomaticCheckingIfEmpty()
    }

    private suspend fun processTrackedEntry(
        key: TrackedKey,
        networksToFetch: MutableSet<SingleNetworkStatusFetcher.Params>,
    ) {
        val entry = trackedEntries[key] ?: return
        if (entry.txIds.isEmpty() && !entry.isInPostConfirmation) {
            trackedEntries.remove(key)
            return
        }

        if (entry.isInPostConfirmation) {
            handlePostConfirmation(key, entry, networksToFetch)
            return
        }

        val pendingTxHashes = yieldSupplyRepository.getPendingTxHashes(
            userWalletId = key.userWalletId,
            cryptoCurrency = entry.cryptoCurrency,
        ).toSet()

        val hasStillPending = entry.txIds.any { it in pendingTxHashes }

        if (hasStillPending) {
            trackedEntries.computeIfPresent(key) { _, current ->
                val newAttempts = current.attempts + 1
                if (newAttempts >= MAX_ATTEMPTS) null else current.copy(attempts = newAttempts)
            }
        } else if (entry.expectedActive != null) {
            trackedEntries[key] = entry.copy(isInPostConfirmation = true, postConfirmationRetries = 0)
        } else {
            trackedEntries.remove(key)
        }

        networksToFetch.add(
            SingleNetworkStatusFetcher.Params(
                userWalletId = key.userWalletId,
                network = entry.cryptoCurrency.network,
            ),
        )
    }

    private suspend fun handlePostConfirmation(
        key: TrackedKey,
        entry: TrackedEntry,
        networksToFetch: MutableSet<SingleNetworkStatusFetcher.Params>,
    ) {
        val expectedActive = entry.expectedActive ?: return

        networksToFetch.add(
            SingleNetworkStatusFetcher.Params(
                userWalletId = key.userWalletId,
                network = entry.cryptoCurrency.network,
            ),
        )

        val currentActive = yieldSupplyRepository.isYieldProtocolActive(
            userWalletId = key.userWalletId,
            cryptoCurrency = entry.cryptoCurrency,
        )

        val statusMatches = currentActive == expectedActive
        val retriesExhausted = entry.postConfirmationRetries >= MAX_POST_CONFIRMATION_RETRIES

        if (statusMatches || retriesExhausted) {
            yieldSupplyRepository.saveTokenProtocolPendingStatus(
                userWalletId = key.userWalletId,
                cryptoCurrency = entry.cryptoCurrency,
                yieldSupplyPendingStatus = null,
            )
            trackedEntries.remove(key)
        } else {
            trackedEntries[key] = entry.copy(postConfirmationRetries = entry.postConfirmationRetries + 1)
        }
    }

    private companion object {
        private const val CHECK_INTERVAL_MS = 10_000L
        private const val MAX_ATTEMPTS = 6
        private const val MAX_POST_CONFIRMATION_RETRIES = 2
    }
}