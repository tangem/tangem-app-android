package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

class ShouldShowStoriesUseCase(private val promoRepository: PromoRepository) {
    operator fun invoke(storyId: String): Flow<Boolean> = promoRepository.isReadyToShowStories(storyId)
    suspend fun invokeSync(storyId: String): Boolean = promoRepository.isReadyToShowStoriesSync(storyId)

    suspend fun neverToShow(storyId: String) = promoRepository.setNeverToShowStories(storyId)
}