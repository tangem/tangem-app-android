package com.tangem.datasource.local.promo

import com.tangem.datasource.api.promotion.models.StoryContentResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import kotlinx.coroutines.flow.Flow

internal class DefaultPromoStoriesStore(
    private val dataStore: StringKeyDataStore<StoryContentResponse>,
) : PromoStoriesStore {
    override suspend fun getSyncOrNull(storyId: String): StoryContentResponse? {
        return dataStore.getSyncOrNull(storyId)
    }

    override fun get(storyId: String): Flow<StoryContentResponse?> {
        return dataStore.get(storyId)
    }

    override suspend fun store(storyId: String, item: StoryContentResponse) {
        dataStore.store(storyId, item)
    }
}