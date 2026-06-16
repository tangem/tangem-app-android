package com.tangem.datasource.local.txhistory.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class DefaultTxHistoryStore(
    private val expressExchangeStore: DataStore<Map<CommonSyncStateKey, CommonSyncState>>,
    private val expressOnrampStore: DataStore<Map<CommonSyncStateKey, CommonSyncState>>,
) : TxHistoryStore {

    override fun expressExchangeSyncState(key: CommonSyncStateKey): Flow<CommonSyncState> {
        return expressExchangeStore.data.map { map -> map.getOrDefault(key) }
    }

    override fun expressOnrampSyncState(key: CommonSyncStateKey): Flow<CommonSyncState> {
        return expressOnrampStore.data.map { map -> map.getOrDefault(key) }
    }

    override suspend fun updateExpressExchangeSyncState(
        key: CommonSyncStateKey,
        value: CommonSyncState,
    ): CommonSyncState {
        return expressExchangeStore.updateData { map -> map.plus(key to value) }
            .getOrDefault(key)
    }

    override suspend fun updateExpressOnrampSyncState(
        key: CommonSyncStateKey,
        value: CommonSyncState,
    ): CommonSyncState {
        return expressOnrampStore.updateData { map -> map.plus(key to value) }
            .getOrDefault(key)
    }

    private fun Map<CommonSyncStateKey, CommonSyncState>.getOrDefault(key: CommonSyncStateKey): CommonSyncState =
        this.getOrDefault(key, CommonSyncState.default(key))
}