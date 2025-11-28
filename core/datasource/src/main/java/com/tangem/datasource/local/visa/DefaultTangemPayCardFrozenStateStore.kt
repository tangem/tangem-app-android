package com.tangem.datasource.local.visa

import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import kotlinx.coroutines.flow.Flow

internal class DefaultTangemPayCardFrozenStateStore(
    private val dataStore: StringKeyDataStore<TangemPayCardFrozenState>,
) : TangemPayCardFrozenStateStore {
    override suspend fun getSyncOrNull(key: String): TangemPayCardFrozenState? {
        return dataStore.getSyncOrNull(key)
    }

    override fun get(key: String): Flow<TangemPayCardFrozenState> {
        return dataStore.get(key)
    }

    override suspend fun store(key: String, value: TangemPayCardFrozenState) {
        dataStore.store(key, value)
    }
}