package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoTokenUseCase(private val promoSettingsRepository: PromoRepository) {

    operator fun invoke(): Flow<Boolean> = promoSettingsRepository.isReadyToShowTokenSwapPromo()

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowTokenSwapPromo()
}