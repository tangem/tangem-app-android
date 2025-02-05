package com.tangem.datasource.local.promo

import com.tangem.datasource.api.promotion.models.StoryContentResponse

interface PromoStoriesStore {
    suspend fun getSyncOrNull(storyId: String): StoryContentResponse?

    suspend fun store(storyId: String, item: StoryContentResponse)
}