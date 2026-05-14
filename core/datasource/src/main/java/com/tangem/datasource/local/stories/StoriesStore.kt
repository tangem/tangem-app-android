package com.tangem.datasource.local.stories

import com.tangem.datasource.api.stories.models.StoryContentResponse
import kotlinx.coroutines.flow.Flow

interface StoriesStore {
    suspend fun getSyncOrNull(storyId: String): StoryContentResponse?

    fun get(storyId: String): Flow<StoryContentResponse?>

    suspend fun store(storyId: String, item: StoryContentResponse)
}