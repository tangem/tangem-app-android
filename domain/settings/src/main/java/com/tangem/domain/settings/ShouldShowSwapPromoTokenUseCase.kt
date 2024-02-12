package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SwapPromoRepository
import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoTokenUseCase(private val swapPromoRepository: SwapPromoRepository) {

    operator fun invoke(): Flow<Boolean> = swapPromoRepository.isReadyToShowTokenPromo()

    suspend fun neverToShow() = swapPromoRepository.setNeverToShowTokenPromo()
}