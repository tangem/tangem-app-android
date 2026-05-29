package com.tangem.datasource.local.stories

import com.tangem.datasource.api.stories.models.StoryContentResponse
import com.tangem.datasource.local.datastore.core.StringKeyDataStore
import kotlinx.coroutines.flow.Flow

internal class DefaultStoriesStore(
    private val dataStore: StringKeyDataStore<StoryContentResponse>,
) : StoriesStore {
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