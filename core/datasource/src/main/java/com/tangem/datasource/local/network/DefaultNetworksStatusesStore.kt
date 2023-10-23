package com.tangem.datasource.local.network

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class DefaultNetworksStatusesStore(
    dataStore: StringKeyDataStore<Set<NetworkStatus>>,
) : NetworksStatusesStore, StringKeyDataStoreDecorator<UserWalletId, Set<NetworkStatus>>(dataStore) {

    private val mutex = Mutex()

    override fun provideStringKey(key: UserWalletId): String {
        return key.stringValue
    }

    override suspend fun store(key: UserWalletId, value: NetworkStatus) {
        mutex.withLock {
            val newValues = getSyncOrNull(key)
                ?.addOrReplace(value) { it.network == value.network }
                ?: setOf(value)

            store(key, newValues)
        }
    }
}
