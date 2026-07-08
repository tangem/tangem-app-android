package com.tangem.data.marketing.store

import androidx.datastore.core.DataStore
import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import kotlinx.coroutines.flow.first

interface MarketingCampaignsCacheStore {
    suspend fun get(type: String): MarketingCampaignsCacheEntry?
    suspend fun store(type: String, entry: MarketingCampaignsCacheEntry)
}

internal class DefaultMarketingCampaignsCacheStore(
    private val dataStore: DataStore<Map<String, MarketingCampaignsCacheEntry>>,
) : MarketingCampaignsCacheStore {

    override suspend fun get(type: String): MarketingCampaignsCacheEntry? = dataStore.data.first()[type]

    override suspend fun store(type: String, entry: MarketingCampaignsCacheEntry) {
        dataStore.updateData { it + (type to entry) }
    }
}