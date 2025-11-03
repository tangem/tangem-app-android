package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultTokenReceiveWarningActionStore(
    private val persistenceStore: DataStore<Set<String>>,
) : TokenReceiveWarningActionStore {

    override suspend fun getSync(): Set<String> {
        return persistenceStore.data.firstOrNull().orEmpty()
    }

    override suspend fun store(symbol: String) {
        persistenceStore.updateData { data -> data.plus(symbol) }
    }
}