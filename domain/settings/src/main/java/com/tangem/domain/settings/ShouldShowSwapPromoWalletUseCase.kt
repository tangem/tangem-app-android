package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.PromoSettingsRepository
import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoWalletUseCase(private val promoSettingsRepository: PromoSettingsRepository) {

    operator fun invoke(): Flow<Boolean> = promoSettingsRepository.isReadyToShowWalletSwapPromo()

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowWalletSwapPromo()
}