package com.tangem.datasource.local.yieldsupply

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.YieldMarketsResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultYieldMarketsStore(
    private val persistenceStore: DataStore<List<YieldMarketsResponse.MarketDto>>,
) : YieldMarketsStore {

    override fun get(): Flow<List<YieldMarketsResponse.MarketDto>> = persistenceStore.data

    override suspend fun getSyncOrNull(): List<YieldMarketsResponse.MarketDto>? {
        return persistenceStore.data.firstOrNull()
    }

    override suspend fun store(items: List<YieldMarketsResponse.MarketDto>) {
        persistenceStore.updateData { _ -> items }
    }
}