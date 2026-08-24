package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.polymarket.converter.PredictionAccountStatusValueDTOConverter
import com.tangem.data.polymarket.entity.PredictionAccountStatusValueDTO
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
internal typealias WalletIdWithPredictionStatusDTO = Map<String, PredictionAccountStatusValueDTO>

/**
 * Store of prediction account statuses, one entry per wallet, with dual storage (runtime + persistence).
 *
 * Holds the value only, never the account itself. What reaches the disk is [PredictionAccountStatusValueDTO],
 * whose shape is pinned independently of the domain type: loading and error states have no representation
 * there, and a restored value always comes back as [StatusSource.CACHE], because it has not been refreshed in
 * this session. The fiat rate is neither stored nor restored — it belongs to the app's selected currency, which
 * the user can change while this cache stays valid, so it is mixed in downstream on every emission.
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
    private val persistenceDataStore: DataStore<WalletIdWithPredictionStatusDTO>,
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

                runtimeStore.store(
                    cached.mapValues { (_, dto) -> PredictionAccountStatusValueDTOConverter.convertBack(dto) },
                )
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
     * Records that a refresh failed. Runtime only and atomic, so it neither survives to the next launch nor
     * overwrites a value a concurrent success has just written. With nothing cached the failure itself is stored:
     * an absent entry reads as [PredictionAccountStatusValue.Loading] and the row shimmers for nothing.
     */
    suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        preloaded.await()

        runtimeStore.update(default = emptyMap()) { stored ->
            val value = stored[userWalletId.stringValue]?.copySealed(source = StatusSource.ONLY_CACHE)
                ?: PredictionAccountStatusValue.Error.Unavailable

            stored + (userWalletId.stringValue to value)
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

    /** A state the DTO cannot represent is one that must not outlive the session, so it leaves the disk copy alone. */
    private suspend fun storeInPersistence(userWalletId: UserWalletId, value: PredictionAccountStatusValue) {
        val dto = PredictionAccountStatusValueDTOConverter.convert(value) ?: return

        persistenceDataStore.updateData { stored ->
            stored + (userWalletId.stringValue to dto)
        }
    }

    private companion object {
        const val TAG = "PredictionAccountStatusStore"
    }
}