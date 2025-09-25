package com.tangem.datasource.local.yieldsupply

import androidx.datastore.core.DataStore
import com.tangem.domain.yield.supply.models.YieldMarketToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

internal class DefaultYieldMarketsStore(
    private val persistenceStore: DataStore<List<YieldMarketToken>>,
) : YieldMarketsStore {

    override fun get(): Flow<List<YieldMarketToken>> = persistenceStore.data

    override suspend fun getSyncOrNull(): List<YieldMarketToken>? {
        return persistenceStore.data.firstOrNull()
    }

    override suspend fun store(items: List<YieldMarketToken>) {
        persistenceStore.updateData { _ -> items }
    }
}