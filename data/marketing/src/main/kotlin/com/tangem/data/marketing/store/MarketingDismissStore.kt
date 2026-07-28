package com.tangem.data.marketing.store

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.first

interface MarketingDismissStore {
    suspend fun getDismissedIds(): Set<Int>
    suspend fun dismiss(id: Int)
}

internal class DefaultMarketingDismissStore(
    private val dataStore: DataStore<Set<Int>>,
) : MarketingDismissStore {

    override suspend fun getDismissedIds(): Set<Int> = dataStore.data.first()

    override suspend fun dismiss(id: Int) {
        dataStore.updateData { it + id }
    }
}