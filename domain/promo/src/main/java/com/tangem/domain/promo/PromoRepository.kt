package com.tangem.domain.promo

import com.tangem.domain.promo.models.StoryContent
import kotlinx.coroutines.flow.Flow

interface PromoRepository {

    // region Promo
    fun isReadyToShowWalletSwapPromo(): Flow<Boolean>

    fun isReadyToShowTokenSwapPromo(): Flow<Boolean>

    suspend fun setNeverToShowWalletSwapPromo()

    suspend fun setNeverToShowTokenSwapPromo()
    // endregion

    // region Stories
    suspend fun getStoryById(id: String): StoryContent

    fun isReadyToShowSwapStories(): Flow<Boolean>

    suspend fun isReadyToShowSwapStoriesSync(): Boolean

    suspend fun setNeverToShowSwapStories()
    // endregion
}