package com.tangem.datasource.local.network

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.datasource.local.datastore.core.StringKeyDataStoreDecorator
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.extensions.addOrReplace

internal class DefaultNetworksStatusesStore(
    dataStore: StringKeyDataStore<Set<NetworkStatus>>,
) : NetworksStatusesStore, StringKeyDataStoreDecorator<UserWalletId, Set<NetworkStatus>>(dataStore) {

    override fun provideStringKey(key: UserWalletId): String {
        return key.stringValue
    }

    override suspend fun store(key: UserWalletId, value: NetworkStatus) {
        val newValues = getSyncOrNull(key)
            ?.addOrReplace(value) { it.network == value.network }
            ?: setOf(value)

        store(key, newValues)
    }
}