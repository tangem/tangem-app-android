package com.tangem.domain.promo

import com.tangem.domain.promo.models.StoryContent
import kotlinx.coroutines.flow.Flow

interface PromoRepository {

    // region Stories
    fun getStoryById(id: String): Flow<StoryContent?>

    suspend fun getStoryByIdSync(id: String, refresh: Boolean): StoryContent?

    fun isReadyToShowStories(storyId: String): Flow<Boolean>

    suspend fun isReadyToShowStoriesSync(storyId: String): Boolean

    suspend fun setNeverToShowStories(storyId: String)
    // endregion
}