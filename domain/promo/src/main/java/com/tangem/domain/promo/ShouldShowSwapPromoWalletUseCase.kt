package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoWalletUseCase(private val promoSettingsRepository: PromoRepository) {

    operator fun invoke(): Flow<Boolean> = promoSettingsRepository.isReadyToShowWalletSwapPromo()

    suspend fun neverToShow() = promoSettingsRepository.setNeverToShowWalletSwapPromo()
}