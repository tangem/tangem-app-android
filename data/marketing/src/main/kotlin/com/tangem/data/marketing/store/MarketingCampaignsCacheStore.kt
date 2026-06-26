package com.tangem.data.marketing.store

import com.tangem.datasource.api.marketing.models.MarketingCampaignsCacheEntry
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectSyncOrNull
import com.tangem.datasource.local.preferences.utils.storeObject

interface MarketingCampaignsCacheStore {
    suspend fun get(type: String): MarketingCampaignsCacheEntry?
    suspend fun store(type: String, entry: MarketingCampaignsCacheEntry)
}

internal class DefaultMarketingCampaignsCacheStore(
    private val appPreferencesStore: AppPreferencesStore,
) : MarketingCampaignsCacheStore {

    override suspend fun get(type: String): MarketingCampaignsCacheEntry? {
        return appPreferencesStore.getObjectSyncOrNull(PreferencesKeys.getMarketingCampaignsCacheKey(type))
    }

    override suspend fun store(type: String, entry: MarketingCampaignsCacheEntry) {
        appPreferencesStore.storeObject(PreferencesKeys.getMarketingCampaignsCacheKey(type), entry)
    }
}