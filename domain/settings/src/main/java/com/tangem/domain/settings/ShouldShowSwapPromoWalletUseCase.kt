package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SwapPromoRepository
import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoWalletUseCase(private val swapPromoRepository: SwapPromoRepository) {

    operator fun invoke(): Flow<Boolean> = swapPromoRepository.isReadyToShowWalletPromo()

    suspend fun neverToShow() = swapPromoRepository.setNeverToShowWalletPromo()
}