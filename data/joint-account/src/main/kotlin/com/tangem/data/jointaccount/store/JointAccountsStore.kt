package com.tangem.data.jointaccount.store

import androidx.datastore.core.DataStore
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.jointaccount.converter.JointAccountDMConverter
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

internal typealias WalletIdWithJointAccounts = Map<String, List<JointAccount>>
internal typealias WalletIdWithJointAccountsDM = Map<String, List<JointAccountDM>>

internal class JointAccountsStore(
    private val runtimeStore: RuntimeSharedStore<WalletIdWithJointAccounts>,
    private val persistenceDataStore: DataStore<WalletIdWithJointAccountsDM>,
    private val converter: JointAccountDMConverter,
    scope: AppCoroutineScope,
) {

    private val logger = TangemLogger.withTag(tag = "JointAccountsStore")

    init {
        scope.launch {
            val hydrated = try {
                persistenceDataStore.data.firstOrNull().orEmpty()
                    .mapValues { (_, accounts) -> accounts.map(converter::convertBack) }
            } catch (error: Exception) {
                logger.e(messageString = "Failed to hydrate, resetting persistence", throwable = error)
                runSuspendCatching { persistenceDataStore.updateData { emptyMap() } }
                emptyMap()
            }

            // Merge under the store's mutex, keeping entries that a faster fetch already put in — hydration must
            // never overwrite fresher data with the persisted copy
            runtimeStore.update(default = emptyMap()) { stored -> hydrated + stored }
        }
    }

    fun get(userWalletId: UserWalletId): Flow<List<JointAccount>?> {
        return runtimeStore.get().map { it[userWalletId.stringValue] }
    }

    suspend fun getSyncOrNull(userWalletId: UserWalletId): List<JointAccount>? {
        return runtimeStore.getSyncOrNull()?.get(userWalletId.stringValue)
    }

    suspend fun contains(userWalletId: UserWalletId): Boolean = getSyncOrNull(userWalletId) != null

    /** The runtime write must succeed; the persistence copy is best-effort — a disk failure must not fail a fetch. */
    suspend fun store(userWalletId: UserWalletId, accounts: List<JointAccount>) {
        storeInRuntime(userWalletId, accounts)

        runSuspendCatching { storeInPersistence(userWalletId, accounts) }
            .onFailure { error -> logger.e(messageString = "Failed to persist", throwable = error) }
    }

    /** Downgrades the freshness of cached data; runtime only — persisted data is always restored as `CACHE`. */
    suspend fun updateStatusSource(userWalletId: UserWalletId, source: StatusSource) {
        runtimeStore.update(default = emptyMap()) { stored ->
            val accounts = stored[userWalletId.stringValue] ?: return@update stored

            stored + (userWalletId.stringValue to accounts.map { it.copy(source = source) })
        }
    }

    private suspend fun storeInRuntime(userWalletId: UserWalletId, accounts: List<JointAccount>) {
        runtimeStore.update(default = emptyMap()) { stored ->
            stored + (userWalletId.stringValue to accounts)
        }
    }

    private suspend fun storeInPersistence(userWalletId: UserWalletId, accounts: List<JointAccount>) {
        persistenceDataStore.updateData { stored ->
            stored + (userWalletId.stringValue to accounts.map(converter::convert))
        }
    }
}