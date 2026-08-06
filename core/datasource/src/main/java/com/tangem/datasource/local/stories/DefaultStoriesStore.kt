package com.tangem.datasource.local.stories

import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.datasource.api.stories.models.StoryContentResponse
import kotlinx.coroutines.flow.Flow

internal class DefaultStoriesStore(
    private val store: RuntimeSharedMapStore<String, StoryContentResponse>,
) : StoriesStore {

    override suspend fun getSyncOrNull(storyId: String): StoryContentResponse? = store.getSyncOrNull(storyId)

    override fun get(storyId: String): Flow<StoryContentResponse?> = store.get(storyId)

    override suspend fun store(storyId: String, item: StoryContentResponse) = store.store(storyId, item)
}