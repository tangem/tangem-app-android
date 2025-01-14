package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

class ShouldShowSwapStoriesUseCase(private val promoRepository: PromoRepository) {
    operator fun invoke(): Flow<Boolean> = promoRepository.isReadyToShowSwapStories()
    suspend fun invokeSync(): Boolean = promoRepository.isReadyToShowSwapStoriesSync()

    suspend fun neverToShow() = promoRepository.setNeverToShowSwapStories()
}