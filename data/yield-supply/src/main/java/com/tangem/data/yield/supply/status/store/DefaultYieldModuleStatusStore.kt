package com.tangem.data.yield.supply.status.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.models.status.YieldModuleStatus
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.extensions.addOrReplace
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal typealias WalletIdWithStatusDMs = Map<String, Set<YieldModuleStatusDM>>
internal typealias WalletIdWithStatuses = Map<UserWalletId, Set<YieldModuleStatus>>

internal class DefaultYieldModuleStatusStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithStatuses>,
    private val persistenceStore: DataStore<WalletIdWithStatusDMs>,
    scope: AppCoroutineScope,
) : YieldModuleStatusStore {

    // Every read and write awaits it: a write during the disk read would be overwritten by the cache (or a removed
    // wallet resurrected), and a read would miss a cached active status
    private val hydration = CompletableDeferred<Unit>()

    // Overlapping writes must reach the runtime store and the disk in the same order
    private val writeMutex = Mutex()

    init {
        scope.launch {
            try {
                hydrate()
            } finally {
                hydration.complete(Unit)
            }
        }
    }

    private suspend fun hydrate() {
        // An unreadable cache must not leave the runtime store empty forever: its flow would never emit
        val cachedStatuses = runSuspendCatching { persistenceStore.data.firstOrNull() }
            .onFailure { TangemLogger.e("Failed to read cached Yield Module statuses", it) }
            .getOrNull()
            .orEmpty()

        val hydrated = cachedStatuses.entries.associate { (stringWalletId, statuses) ->
            UserWalletId(stringWalletId) to statuses.toDomain()
        }

        runtimeStore.update(default = emptyMap()) { stored ->
            (hydrated.keys + stored.keys).associateWith { userWalletId ->
                val fresh = stored[userWalletId].orEmpty()

                hydrated[userWalletId]?.addOrReplace(items = fresh) { old, new -> old.key == new.key } ?: fresh
            }
        }
    }

    override fun get(userWalletId: UserWalletId): Flow<Set<YieldModuleStatus>> = flow {
        hydration.await()

        emitAll(runtimeStore.get().map { it[userWalletId].orEmpty() })
    }

    override suspend fun getSync(userWalletId: UserWalletId): Set<YieldModuleStatus> {
        hydration.await()

        return runtimeStore.getSyncOrNull()?.get(userWalletId).orEmpty()
    }

    override suspend fun refresh(userWalletId: UserWalletId, keys: Set<YieldModuleStatus.Key>) {
        updateSourceInRuntime(userWalletId = userWalletId, keys = keys, source = StatusSource.CACHE)
    }

    override suspend fun storeActual(userWalletId: UserWalletId, statuses: Set<YieldModuleStatus>) {
        if (statuses.isEmpty()) return
        hydration.await()

        val actualStatuses = statuses.mapTo(hashSetOf()) { it.copy(source = StatusSource.ACTUAL) }

        writeMutex.withLock {
            storeInRuntime(userWalletId = userWalletId, statuses = actualStatuses)
            storeInPersistence(userWalletId = userWalletId, statuses = actualStatuses)
        }
    }

    override suspend fun storeError(userWalletId: UserWalletId, keys: Set<YieldModuleStatus.Key>) {
        updateSourceInRuntime(userWalletId = userWalletId, keys = keys, source = StatusSource.ONLY_CACHE)
    }

    override suspend fun clear(userWalletId: UserWalletId, keys: Set<YieldModuleStatus.Key>) {
        if (keys.isEmpty()) return
        hydration.await()

        writeMutex.withLock {
            runtimeStore.update(default = emptyMap()) { stored ->
                val statuses = stored[userWalletId] ?: return@update stored
                stored + (userWalletId to statuses.filterNotTo(hashSetOf()) { it.key in keys })
            }

            persistenceStore.updateData { stored ->
                val statuses = stored[userWalletId.stringValue] ?: return@updateData stored
                stored + (userWalletId.stringValue to statuses.filterNotTo(hashSetOf()) { it.key in keys })
            }
        }
    }

    override suspend fun remove(userWalletIds: List<UserWalletId>) {
        if (userWalletIds.isEmpty()) return
        hydration.await()

        val walletIds = userWalletIds.toSet()
        val stringKeys = userWalletIds.mapTo(hashSetOf()) { it.stringValue }

        writeMutex.withLock {
            runSuspendCatching { runtimeStore.update(default = emptyMap()) { stored -> stored - walletIds } }
                .onFailure { TangemLogger.e("Failed to remove Yield Module statuses from runtime", it) }
            runSuspendCatching { persistenceStore.updateData { stored -> stored - stringKeys } }
                .onFailure { TangemLogger.e("Failed to remove Yield Module statuses from persistence", it) }
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, statuses: Set<YieldModuleStatus>) {
        runtimeStore.update(default = emptyMap()) { stored ->
            val updated = stored[userWalletId]
                ?.addOrReplace(items = statuses) { old, new -> old.key == new.key }
                ?: statuses

            stored + (userWalletId to updated)
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, statuses: Set<YieldModuleStatus>) {
        val dataModels = statuses.mapTo(hashSetOf(), YieldModuleStatus::toDataModel)

        persistenceStore.updateData { stored ->
            val updated = stored[userWalletId.stringValue]
                ?.addOrReplace(items = dataModels) { old, new -> old.key == new.key }
                ?: dataModels

            stored + (userWalletId.stringValue to updated)
        }
    }

    private fun Set<YieldModuleStatusDM>.toDomain(): Set<YieldModuleStatus> {
        return mapTo(hashSetOf()) { it.toDomain(StatusSource.CACHE) }
    }

    private suspend fun updateSourceInRuntime(
        userWalletId: UserWalletId,
        keys: Set<YieldModuleStatus.Key>,
        source: StatusSource,
    ) {
        if (keys.isEmpty()) return
        hydration.await()

        runtimeStore.update(default = emptyMap()) { stored ->
            val statuses = stored[userWalletId] ?: return@update stored

            val updated = statuses.mapTo(hashSetOf()) { if (it.key in keys) it.copy(source = source) else it }

            stored + (userWalletId to updated)
        }
    }
}