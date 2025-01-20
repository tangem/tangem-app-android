package com.tangem.domain.promo

import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoWalletUseCase(private val promoRepository: PromoRepository) {

    operator fun invoke(): Flow<Boolean> = promoRepository.isReadyToShowWalletSwapPromo()

    suspend fun neverToShow() = promoRepository.setNeverToShowWalletSwapPromo()
}