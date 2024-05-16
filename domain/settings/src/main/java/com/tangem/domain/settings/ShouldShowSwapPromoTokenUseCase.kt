package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.PromoSettingsRepository
import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoTokenUseCase(private val promoSettingsRepository: PromoSettingsRepository) {

    operator fun invoke(): Flow<Boolean> = promoSettingsRepository.isReadyToShowTokenSwapPromo()

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowTokenSwapPromo()
}