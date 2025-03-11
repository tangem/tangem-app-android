package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoTokenUseCase(private val promoRepository: PromoRepository) {

    operator fun invoke(): Flow<Boolean> = promoRepository.isReadyToShowTokenSwapPromo()

    suspend fun neverToShow() = promoRepository.setNeverToShowTokenSwapPromo()
}