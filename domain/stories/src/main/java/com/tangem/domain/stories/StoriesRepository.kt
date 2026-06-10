package com.tangem.domain.stories

import com.tangem.domain.stories.models.StoryContent
import kotlinx.coroutines.flow.Flow

interface StoriesRepository {

    // region Stories
    fun getStoryById(id: String): Flow<StoryContent?>

    suspend fun getStoryByIdSync(id: String, refresh: Boolean): StoryContent?

    fun isReadyToShowStories(storyId: String): Flow<Boolean>

    suspend fun isReadyToShowStoriesSync(storyId: String): Boolean

    suspend fun setNeverToShowStories(storyId: String)
    // endregion
}