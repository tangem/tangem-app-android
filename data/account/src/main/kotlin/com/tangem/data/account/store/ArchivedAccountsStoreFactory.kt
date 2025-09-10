package com.tangem.data.account.store

import androidx.annotation.VisibleForTesting
import com.tangem.datasource.local.datastore.RuntimeStateStore
import com.tangem.domain.models.wallet.UserWalletId
import java.util.concurrent.ConcurrentHashMap

/**
 * Factory for creating and managing instances of [ArchivedAccountsStore].

 * and reused for each unique [UserWalletId].
 *
[REDACTED_AUTHOR]
 */
internal object ArchivedAccountsStoreFactory {

    private val createdRuntimeStores = ConcurrentHashMap<UserWalletId, ArchivedAccountsStore>()

    /**
     * Creates or retrieves an existing instance of [ArchivedAccountsStore] for the given [userWalletId].
     *
     * @param userWalletId the unique identifier for the user wallet
     */
    fun create(userWalletId: UserWalletId): ArchivedAccountsStore {
        return createdRuntimeStores.computeIfAbsent(userWalletId) {
            ArchivedAccountsStore(runtimeStore = RuntimeStateStore(defaultValue = null))
        }
    }

    @VisibleForTesting
    fun getAllStores(): Map<UserWalletId, ArchivedAccountsStore> = createdRuntimeStores.toMap()

    @VisibleForTesting
    fun clearStores() {
        createdRuntimeStores.clear()
    }
}