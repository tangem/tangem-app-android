package com.tangem.domain.yield.supply.usecase

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.domain.yield.supply.YieldSupplyTransactionRepository
import com.tangem.domain.yield.supply.models.YieldSupplyEnterOutcome
import com.tangem.domain.yield.supply.usecase.YieldSupplyPendingTracker.Companion.CHECK_INTERVAL_MS
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
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
 * - The periodic job stops automatically when there is nothing left to track.
 *
 * When an entry carries a [DepositContext], the tracker also enrols the module in auto top-up by telling the
 * backend it is active. That call is what dispatches the backend relayer, and the relayer performs the same
 * deposit as the client, so it is deferred until the client's own deposit has settled — announcing it while
 * that transaction is still in flight makes the two race, and the loser reverts with `ZeroAmount()`.
 */
class YieldSupplyPendingTracker(
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val yieldSupplyTransactionRepository: YieldSupplyTransactionRepository,
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher,
    private val coroutineScope: AppCoroutineScope,
) {

    /**
     * Everything needed to enrol the module in auto top-up once the activation has settled.
     *
     * @param enterTransactionHash the client's own deposit, or null when there was nothing to deposit — in
     * which case nothing can race the relayer and enrolment happens as soon as the setup transactions settle.
     */
    data class DepositContext(
        val ownerAddress: String,
        val enterTransactionHash: String?,
    )

    private data class TrackedKey(
        val userWalletId: UserWalletId,
        val cryptoCurrencyId: CryptoCurrency.ID,
    )

    private data class TrackedEntry(
        val cryptoCurrency: CryptoCurrency,
        val txIds: Set<String>,
        val depositContext: DepositContext? = null,
        val attempts: Int = 0,
    )

    private val trackedEntries = ConcurrentHashMap<TrackedKey, TrackedEntry>()
    private val mutex = Mutex()
    private var checkingJob: Job? = null

    /**
     * Add tx ids to track for a wallet/currency pair. Starts periodic checking if needed.
     *
     * @param depositContext set for the activation flow, to defer auto top-up enrolment until the client's
     * own deposit has settled. Left null by flows that enrol nothing, such as a standalone approve or a stop.
     * It replaces any context already tracked: a stop requested while a deposit is still settling must cancel
     * that enrolment, otherwise the settled deposit would re-activate a protocol the user just deactivated.
     */
    suspend fun addPending(
        userWalletId: UserWalletId,
        cryptoCurrency: CryptoCurrency,
        txIds: List<String>,
        depositContext: DepositContext? = null,
    ) {
        val key = TrackedKey(userWalletId, cryptoCurrency.id)
        trackedEntries.compute(key) { _, existing ->
            if (existing == null) {
                TrackedEntry(
                    cryptoCurrency = cryptoCurrency,
                    txIds = txIds.toSet(),
                    depositContext = depositContext,
                )
            } else {
                existing.copy(
                    txIds = existing.txIds + txIds,
                    depositContext = depositContext,
                )
            }
        }

        startAutomaticCheckingIfNeeded()
    }

    /**
     * True while the client's own activation deposit is still settling. Auto top-up enrolment belongs to this
     * tracker for that whole window, so nothing else may announce the protocol as active.
     */
    fun hasPendingDeposit(userWalletId: UserWalletId, cryptoCurrencyId: CryptoCurrency.ID): Boolean {
        return trackedEntries[TrackedKey(userWalletId, cryptoCurrencyId)]?.depositContext != null
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
            val entry = trackedEntries[key] ?: continue
            if (entry.txIds.isEmpty()) {
                trackedEntries.remove(key)
                continue
            }

            val isSettled = entry.depositContext
                ?.let { settleDeposit(key, entry, it) }
                ?: isNoLongerPending(key, entry)

            if (isSettled) {
                trackedEntries.remove(key)
            } else {
                trackedEntries.computeIfPresent(key) { _, current ->
                    val newAttempts = current.attempts + 1
                    if (newAttempts >= MAX_ATTEMPTS) null else current.copy(attempts = newAttempts)
                }
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

    private suspend fun isNoLongerPending(key: TrackedKey, entry: TrackedEntry): Boolean {
        val pendingTxHashes = yieldSupplyRepository
            .getPendingTxHashes(userWalletId = key.userWalletId, cryptoCurrency = entry.cryptoCurrency)
            .toSet()

        return entry.txIds.none { it in pendingTxHashes }
    }

    /**
     * @return true once auto top-up enrolment has been decided, false while the deposit is still in flight.
     */
    private suspend fun settleDeposit(key: TrackedKey, entry: TrackedEntry, depositContext: DepositContext): Boolean {
        val token = entry.cryptoCurrency as? CryptoCurrency.Token ?: return true
        val ownerAddress = depositContext.ownerAddress

        val enterTransactionHash = depositContext.enterTransactionHash
            // Nothing was deposited, so nothing can race the relayer. Enrol as soon as the setup transactions
            // settle, so funds arriving later are supplied automatically.
            ?: return isNoLongerPending(key, entry).also { isSettled ->
                if (isSettled) activateProtocol(key, token, ownerAddress)
            }

        val outcome = yieldSupplyTransactionRepository.getEnterTransactionOutcome(
            userWalletId = key.userWalletId,
            cryptoCurrency = token,
            transactionHash = enterTransactionHash,
        ) ?: return false

        when (outcome) {
            // Announcing the module now is safe: our own deposit has landed, so the relayer will find nothing
            // to move and stand down.
            YieldSupplyEnterOutcome.Deposited -> activateProtocol(key, token, ownerAddress)
            // The funds are still on the owner address, so the relayer is the only remaining way in.
            YieldSupplyEnterOutcome.Failed -> {
                TangemLogger.e("Yield enter transaction $enterTransactionHash reverted, funds not deposited")
                activateProtocol(key, token, ownerAddress)
            }
            // The relayer already deposited, so the backend knows and there is nothing to announce.
            YieldSupplyEnterOutcome.DepositedByRelayer -> Unit
        }

        return true
    }

    private suspend fun activateProtocol(key: TrackedKey, token: CryptoCurrency.Token, ownerAddress: String) {
        runSuspendCatching {
            yieldSupplyRepository.activateProtocol(
                userWalletId = key.userWalletId,
                cryptoCurrencyToken = token,
                address = ownerAddress,
            )
        }.onFailure { TangemLogger.e("Failed to activate yield protocol", it) }
    }

    private companion object {
        private const val CHECK_INTERVAL_MS = 10_000L
        private const val MAX_ATTEMPTS = 15
    }
}