package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.yield.supply.YieldSupplyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Periodically checks pending Yield Supply transaction hashes for a given set of wallet/currency pairs.
 *
 * - Call [addPending] right after sending a transaction to start tracking its hashes.
 * - The checker runs every [CHECK_INTERVAL_MS] and queries repository for the current pending tx hashes.
 * - If none of the stored hashes are pending anymore for a tracked entry, it triggers a single-network status refresh
 *   via [singleNetworkStatusFetcher] and removes the entry from tracking.
 * - The periodic job stops automatically when there is nothing left to track.
 */
class YieldSupplyPendingTracker(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val coroutineScope: CoroutineScope,
) {

    private data class TrackedKey(
        val userWalletId: UserWalletId,
        val cryptoCurrencyId: CryptoCurrency.ID,
    )

    private data class TrackedEntry(
        val cryptoCurrency: CryptoCurrency,
        val txIds: Set<String>,
        val attempts: Int = 0,
    )

    private val trackedEntries = ConcurrentHashMap<TrackedKey, TrackedEntry>()
    private val mutex = Mutex()
    private var checkingJob: Job? = null

    /**
     * Add tx ids to track for a wallet/currency pair. Starts periodic checking if needed.
     */
    suspend fun addPending(userWalletId: UserWalletId, cryptoCurrency: CryptoCurrency, txIds: List<String>) {
        val key = TrackedKey(userWalletId, cryptoCurrency.id)
        trackedEntries.compute(key) { _, existing ->
            if (existing == null) {
                TrackedEntry(
                    cryptoCurrency = cryptoCurrency,
                    txIds = txIds.toSet(),
                )
            } else {
                existing.copy(txIds = existing.txIds + txIds)
            }
        }

        startAutomaticCheckingIfNeeded()
    }

    private suspend fun startAutomaticCheckingIfNeeded() {
        mutex.withLock {
            if (checkingJob?.isActive == true) return

            checkingJob = coroutineScope.launch {
                while (isActive) {
                    try {
                        checkAllTracked()
                    } catch (ex: Exception) {
                        Timber.e(ex)
                    }
                    delay(CHECK_INTERVAL_MS)
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
            val entry = trackedEntries[key] ?: continue
            if (entry.txIds.isEmpty()) {
                trackedEntries.remove(key)
                continue
            }

            val pendingTxHashes =
                yieldSupplyRepository.getPendingTxHashes(
                    userWalletId = key.userWalletId,
                    cryptoCurrency = entry.cryptoCurrency,
                )
                    .toSet()

            val hasStillPending = entry.txIds.any { it in pendingTxHashes }

            if (hasStillPending) {
                trackedEntries.computeIfPresent(key) { _, current ->
                    val newAttempts = current.attempts + 1
                    if (newAttempts >= MAX_ATTEMPTS) null else current.copy(attempts = newAttempts)
                }
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

        for (params in networksToFetch) {
            singleNetworkStatusFetcher(params)
        }

        stopAutomaticCheckingIfEmpty()
    }

    private companion object {
        private const val CHECK_INTERVAL_MS = 10_000L
        private const val MAX_ATTEMPTS = 6
    }
}