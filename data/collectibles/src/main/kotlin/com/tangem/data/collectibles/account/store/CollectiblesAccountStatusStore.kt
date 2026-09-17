package com.tangem.data.collectibles.account.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.entity.CollectiblesAccountStatusValueDTO
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.domain.models.account.CollectiblesVaultBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal typealias WalletIdWithCollectiblesStatus = Map<String, CollectiblesAccountStatusValue>
internal typealias WalletIdWithCollectiblesStatusDTO = Map<String, CollectiblesAccountStatusValueDTO>

/**
 * Store of collectibles account statuses, one entry per wallet, with dual storage (runtime + persistence).
 *
 * Holds the value only, never the account itself. What reaches the disk is [CollectiblesAccountStatusValueDTO] —
 * see its documentation for which states are persisted at all, and why the source and the rates are not among
 * them.
 *
 * [get] emits on subscription even when nothing has ever been stored. That is not a convenience: the status is
 * combined with the other accounts' statuses, and `combine` withholds every value until all of its sources have
 * emitted at least once — a silent source here would stall the whole wallet.
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 * @property converter            maps the status half between its domain and storage shapes
 */
internal class CollectiblesAccountStatusStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithCollectiblesStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithCollectiblesStatusDTO>,
    private val converter: CollectiblesAccountStatusValueDTOConverter,
    scope: AppCoroutineScope,
) {

    /**
     * Completed once the persisted statuses have been loaded. Writes and synchronous reads wait for it — a
     * wallet deleted during startup cannot be resurrected by the preload, and a caller asking what is stored
     * cannot be told "nothing" while the disk is still being read. [get] does not wait: it must stay immediate.
     */
    private val preloaded = CompletableDeferred<Unit>()

    /**
     * Holds the runtime and the disk halves of a write together. Without it a [clear] can land between them, and
     * the write's second half then puts a deleted wallet's account back on disk, where nothing will remove it
     * again. Taken after [preloaded], never nested — [Mutex] is not reentrant.
     */
    private val writeMutex = Mutex()

    init {
        scope.launch {
            try {
                val cached = runSuspendCatching { persistenceDataStore.data.first() }
                    .onFailure { logger.e("Failed to read the cached collectibles account statuses", it) }
                    .getOrDefault(emptyMap())

                runtimeStore.store(cached.mapValues { (_, dto) -> converter.convertBack(dto) })
            } finally {
                preloaded.complete(Unit)
            }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<CollectiblesAccountStatusValue?> {
        return runtimeStore.get()
            .onStart { emit(runtimeStore.getSyncOrDefault(emptyMap())) }
            .map { statuses -> statuses[userWalletId.stringValue] }
            .distinctUntilChanged()
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): CollectiblesAccountStatusValue? {
        preloaded.await()

        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    /**
     * Records that the wallet has an account, in the state [value] describes. A [value] that means anything else
     * is refused: a loading or failed read may not be recorded as the account itself.
     *
     * A [value] carrying no balance keeps the one already cached — the record and the amount are read separately,
     * and re-reading the record must not blank an amount that is still valid.
     */
    suspend fun store(userWalletId: UserWalletId, value: CollectiblesAccountStatusValue) {
        if (converter.convert(value) == null) {
            logger.e("Refusing to record $value as a collectibles account of $userWalletId")
            return
        }

        write(userWalletId) { stored -> value.copySealed(vaultBalance = value.balance ?: stored?.balance) }
    }

    /** Records a fresh read of the vault. Nothing recorded is no account, so there is nothing to attach it to. */
    suspend fun storeBalance(userWalletId: UserWalletId, balance: CollectiblesVaultBalance) {
        write(userWalletId) { stored ->
            stored?.copySealed(source = StatusSource.ACTUAL, vaultBalance = balance)
        }
    }

    /**
     * Records that a refresh failed. Runtime only: whatever is cached stays and is marked stale, so a wallet
     * whose vault could not be read keeps its last known amount instead of dropping it out of the wallet total.
     */
    suspend fun markUnrefreshed(userWalletId: UserWalletId) {
        preloaded.await()

        runtimeStore.update(default = emptyMap()) { stored ->
            val value = stored[userWalletId.stringValue] ?: return@update stored

            stored + (userWalletId.stringValue to value.copySealed(source = StatusSource.ONLY_CACHE))
        }
    }

    suspend fun clear(userWalletId: UserWalletId) {
        preloaded.await()

        writeMutex.withLock {
            runtimeStore.update(default = emptyMap()) { it - userWalletId.stringValue }

            runSuspendCatching { persistenceDataStore.updateData { it - userWalletId.stringValue } }
                .onFailure { logger.e("Failed to clear the persisted collectibles account of $userWalletId", it) }
        }
    }

    /** Applies [update] to the wallet's entry in the runtime store, then persists what it produced. */
    private suspend fun write(
        userWalletId: UserWalletId,
        update: (CollectiblesAccountStatusValue?) -> CollectiblesAccountStatusValue?,
    ) {
        preloaded.await()

        writeMutex.withLock {
            var written: CollectiblesAccountStatusValue? = null

            runtimeStore.update(default = emptyMap()) { stored ->
                val updated = update(stored[userWalletId.stringValue]) ?: return@update stored
                written = updated

                stored + (userWalletId.stringValue to updated)
            }

            val dto = written?.let(converter::convert) ?: return@withLock

            runSuspendCatching {
                persistenceDataStore.updateData { stored -> stored + (userWalletId.stringValue to dto) }
            }.onFailure { logger.e("Failed to persist the collectibles account of $userWalletId", it) }
        }
    }

    private companion object {
        val logger = TangemLogger.withTag("CollectiblesAccountStatusStore")
    }
}