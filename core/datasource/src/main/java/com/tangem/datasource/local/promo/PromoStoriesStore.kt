package com.tangem.datasource.local.promo

import com.tangem.datasource.api.promotion.models.StoryContentResponse
import kotlinx.coroutines.flow.Flow

interface PromoStoriesStore {
    suspend fun getSyncOrNull(storyId: String): StoryContentResponse?

    fun get(storyId: String): Flow<StoryContentResponse?>

    suspend fun store(storyId: String, item: StoryContentResponse)
}