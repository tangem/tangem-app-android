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

            // Merged, not replaced: a refresh that lands while the disk is being read must not be rolled back,
            // and a wallet deleted in that window must not come back from the snapshot taken before the deletion
            runtimeStore.update(default = emptyMap()) { current -> cached + current }
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
        val unpriced = value.withoutFiatRate()

        coroutineScope {
            launch { storeInRuntime(userWalletId = userWalletId, value = unpriced) }
            launch { storeInPersistence(userWalletId = userWalletId, value = unpriced) }
        }
    }

    /**
     * Marks what is already cached as un-refreshed. Runtime only, and in one atomic update: a refresh that failed
     * must not overwrite the value a concurrent successful one has just written, and a source saying "could not be
     * refreshed" must not survive to the next launch, where nothing has been attempted yet.
     */
    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        runtimeStore.update(default = emptyMap()) { stored ->
            val value = stored[userWalletId.stringValue] ?: return@update stored

            stored + (userWalletId.stringValue to value.copySealed(source = source))
        }
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

    /** Enforces the "no rate here" contract on write rather than trusting every caller to honour it. */
    private fun PredictionAccountStatusValue.withoutFiatRate(): PredictionAccountStatusValue {
        return if (this is PredictionAccountStatusValue.Active) copy(fiatRate = null) else this
    }

    private companion object {
        const val TAG = "PredictionAccountStatusStore"
    }
}