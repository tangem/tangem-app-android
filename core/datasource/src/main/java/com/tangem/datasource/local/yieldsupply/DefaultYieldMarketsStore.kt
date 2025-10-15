package com.tangem.datasource.local.yieldsupply

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.tangemTech.models.YieldSupplyMarketTokenDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultYieldMarketsStore(
    private val persistenceStore: DataStore<List<YieldSupplyMarketTokenDto>>,
) : YieldMarketsStore {

    override fun get(): Flow<List<YieldSupplyMarketTokenDto>> = persistenceStore.data

    override suspend fun getSyncOrNull(): List<YieldSupplyMarketTokenDto>? {
        return persistenceStore.data.firstOrNull()
    }

    override suspend fun store(items: List<YieldSupplyMarketTokenDto>) {
        persistenceStore.updateData { _ -> items }
    }
}