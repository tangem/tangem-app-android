package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias WalletIdWithPredictionStatus = Map<String, PredictionAccountStatusValue>

/**
 * Store of prediction account statuses, one entry per wallet, with dual storage (runtime + persistence).
 *
 * Holds the value only, never the account itself. The value it is given carries no fiat rate: the rate belongs
 * to the app's selected currency, which can change while this cache stays valid, so the fetcher writes an
 * unpriced balance and the rate is mixed in downstream, on every emission.
 *
 * [get] emits on subscription even when nothing has ever been stored. That is not a convenience: the status is
 * combined with other accounts' statuses, and `combine` withholds every value until all of its sources have
 * emitted at least once — a silent source here would stall the whole wallet.
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 */
internal class PredictionAccountStatusStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithPredictionStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithPredictionStatus>,
    scope: AppCoroutineScope,
) {

    private val logger = TangemLogger.withTag(TAG)

    /**
     * Completed once the persisted statuses have been loaded. Every write waits for it, so no write can be
     * undone by a snapshot of the disk taken before it — in particular a wallet deleted during startup cannot
     * be resurrected by the preload, which is the whole reason the deletion cleanup exists.
     *
     * Reads deliberately do not wait: [get] must stay immediate.
     */
    private val preloaded = CompletableDeferred<Unit>()

    init {
        scope.launch {
            try {
                val cached = runSuspendCatching { persistenceDataStore.data.first() }
                    .onFailure { logger.e("Failed to read the cached prediction account statuses", it) }
                    .getOrDefault(emptyMap())

                runtimeStore.store(cached)
            } finally {
                preloaded.complete(Unit)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<PredictionAccountStatusValue?> {
        return runtimeStore.get()
            .onStart { emit(runtimeStore.getSyncOrDefault(emptyMap())) }
            .map { statuses -> statuses[userWalletId.stringValue] }
            .distinctUntilChanged()
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): PredictionAccountStatusValue? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    suspend fun store(userWalletId: UserWalletId, value: PredictionAccountStatusValue) {
        preloaded.await()

        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, value = value) }
            launch { storeInPersistence(userWalletId = userWalletId, value = value) }
        }
    }

    /**
     * Marks what is already cached as un-refreshed. Runtime only, and in one atomic update: a refresh that failed
     * must not overwrite the value a concurrent successful one has just written, and a source saying "could not be
     * refreshed" must not survive to the next launch, where nothing has been attempted yet.
     */
    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        preloaded.await()

        runtimeStore.update(default = emptyMap()) { stored ->
            val value = stored[userWalletId.stringValue] ?: return@update stored

            stored + (userWalletId.stringValue to value.copySealed(source = source))
        }
    }

    suspend fun clear(userWalletId: UserWalletId) {
        preloaded.await()

        coroutineScope {
            launch { runtimeStore.update(default = emptyMap()) { it - userWalletId.stringValue } }
            launch { persistenceDataStore.updateData { it - userWalletId.stringValue } }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, value: PredictionAccountStatusValue) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored + (userWalletId.stringValue to value)
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, value: PredictionAccountStatusValue) {
        persistenceDataStore.updateData { stored ->
            stored + (userWalletId.stringValue to value)
        }
    }

    private companion object {
        const val TAG = "PredictionAccountStatusStore"
    }
}