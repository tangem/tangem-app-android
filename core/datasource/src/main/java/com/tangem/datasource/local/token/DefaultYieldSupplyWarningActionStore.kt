package com.tangem.datasource.local.token

import androidx.datastore.core.DataStore
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

internal class DefaultYieldSupplyWarningActionStore(
    private val persistenceStore: DataStore<Set<String>>,
    private val dispatchers: CoroutineDispatcherProvider,
) : YieldSupplyWarningActionStore {

    override suspend fun getSync(): Set<String> = withContext(dispatchers.io) {
        persistenceStore.data.firstOrNull() ?: emptySet()
    }

    override suspend fun store(symbol: String) = withContext(dispatchers.io) {
        persistenceStore.updateData { data -> data.plus(symbol) }
        return@withContext
    }
}