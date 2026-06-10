package com.tangem.datasource.local.txhistory.store

import kotlinx.coroutines.flow.Flow

interface TxHistoryStore {

    fun expressExchangeSyncState(key: CommonSyncStateKey): Flow<CommonSyncState>
    fun expressOnrampSyncState(key: CommonSyncStateKey): Flow<CommonSyncState>

    suspend fun updateExpressExchangeSyncState(key: CommonSyncStateKey, value: CommonSyncState): CommonSyncState
    suspend fun updateExpressOnrampSyncState(key: CommonSyncStateKey, value: CommonSyncState): CommonSyncState
}