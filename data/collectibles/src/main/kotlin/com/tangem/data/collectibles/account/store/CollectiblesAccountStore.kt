package com.tangem.data.collectibles.account.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.collectibles.account.converter.CollectiblesAccountStatusValueDTOConverter
import com.tangem.data.collectibles.account.entity.CollectiblesAccountDTO
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
internal typealias WalletIdWithCollectiblesAccountDTO = Map<String, CollectiblesAccountDTO>

/**
 * Store of collectibles accounts, one entry per wallet, with dual storage (runtime + persistence).
 *
 * An entry has two halves, written independently. Only the status half is mirrored in the runtime store —
 * nothing observes the delegate. Holding both in one entry is what makes [clear] drop them together.
 *
 * [get] emits on subscription even when nothing has ever been stored. That is not a convenience: the status is
 * combined with the other accounts' statuses, and `combine` withholds every value until all of its sources have
 * emitted at least once — a silent source here would stall the whole wallet.
 *
 * @property runtimeStore         runtime store for fast in-memory access
 * @property persistenceDataStore persistence store for caching across app restarts
 * @property converter            maps the status half between its domain and storage shapes
 */
internal class CollectiblesAccountStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithCollectiblesStatus>,
    private val persistenceDataStore: DataStore<WalletIdWithCollectiblesAccountDTO>,
    private val converter: CollectiblesAccountStatusValueDTOConverter,
    scope: AppCoroutineScope,
) {

    /** Completed once the disk has been read. Writes and synchronous reads wait for it; [get] must stay immediate. */
    private val preloaded = CompletableDeferred<Unit>()

    /** Holds both halves of a write together, so a [clear] cannot land between them. Never nested — not reentrant. */
    private val writeMutex = Mutex()

    init {
        scope.launch {
            try {
                val cached = runSuspendCatching { persistenceDataStore.data.first() }
                    .onFailure { logger.e("Failed to read the cached collectibles accounts", it) }
                    .getOrDefault(emptyMap())

                runtimeStore.store(
                    cached.mapNotNull { (walletId, dto) ->
                        dto.status?.let { walletId to converter.convertBack(it) }
                    }.toMap(),
                )
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

    suspend fun getDelegate(userWalletId: UserWalletId): String? {
        preloaded.await()

        return runSuspendCatching {
            persistenceDataStore.data.first()[userWalletId.stringValue]?.delegateHotWalletId
        }
            .onFailure { logger.e("Failed to read the collectibles delegate of $userWalletId", it) }
            .getOrNull()
    }

    /** Throws when the write does not reach the disk: an unrecorded key is a keystore entry nothing can reach. */
    suspend fun storeDelegate(userWalletId: UserWalletId, hotWalletId: String) {
        preloaded.await()

        writeMutex.withLock {
            persist(userWalletId) { stored -> stored.copy(delegateHotWalletId = hotWalletId) }.getOrThrow()
        }
    }

    suspend fun removeDelegate(userWalletId: UserWalletId) {
        preloaded.await()

        writeMutex.withLock {
            persist(userWalletId) { stored -> stored.copy(delegateHotWalletId = null) }.getOrThrow()
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

            persist(userWalletId) { stored -> stored.copy(status = dto) }
        }
    }

    /** Must be called under [writeMutex]: a [clear] landing mid-write would otherwise be undone by it. */
    private suspend fun persist(
        userWalletId: UserWalletId,
        update: (CollectiblesAccountDTO) -> CollectiblesAccountDTO,
    ): Result<WalletIdWithCollectiblesAccountDTO> {
        return runSuspendCatching {
            persistenceDataStore.updateData { stored ->
                val entry = update(stored[userWalletId.stringValue] ?: CollectiblesAccountDTO())

                stored + (userWalletId.stringValue to entry)
            }
        }.onFailure { logger.e("Failed to persist the collectibles account of $userWalletId", it) }
    }

    private companion object {
        val logger = TangemLogger.withTag("CollectiblesAccountStore")
    }
}