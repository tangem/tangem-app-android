package com.tangem.data.polymarket.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal typealias WalletIdWithPredictionStatus = Map<String, PredictionAccountStatusValue>

/**
 * Store of prediction account statuses, one entry per wallet, with dual storage (runtime + persistence).
 *
 * Holds the value only — the balance and the structure — never the account itself and never the fiat rate: the
 * rate belongs to the app's selected currency, which can change while this cache stays valid, so it is mixed in
 * downstream instead of being frozen here.
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

    init {
        scope.launch {
            val cached = runSuspendCatching { persistenceDataStore.data.first() }
                .onFailure { logger.e("Failed to read the cached prediction account statuses", it) }
                .getOrDefault(emptyMap())

            runtimeStore.store(cached)
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
        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, value = value) }
            launch { storeInPersistence(userWalletId = userWalletId, value = value) }
        }
    }

    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        val updated = runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)?.copySealed(source = source) ?: return

        store(userWalletId = userWalletId, value = updated)
    }

    suspend fun clear(userWalletId: UserWalletId) {
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