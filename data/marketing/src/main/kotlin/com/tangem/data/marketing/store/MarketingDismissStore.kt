package com.tangem.data.marketing.store

import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getObjectListSync
import com.tangem.datasource.local.preferences.utils.storeObjectList

interface MarketingDismissStore {
    suspend fun getDismissedIds(): Set<Int>
    suspend fun dismiss(id: Int)
}

internal class DefaultMarketingDismissStore(
    private val appPreferencesStore: AppPreferencesStore,
) : MarketingDismissStore {

    override suspend fun getDismissedIds(): Set<Int> {
        return appPreferencesStore.getObjectListSync<Int>(PreferencesKeys.MARKETING_DISMISSED_BANNER_IDS_KEY).toSet()
    }

    override suspend fun dismiss(id: Int) {
        val current = getDismissedIds()
        appPreferencesStore.storeObjectList(PreferencesKeys.MARKETING_DISMISSED_BANNER_IDS_KEY, (current + id).toList())
    }
}