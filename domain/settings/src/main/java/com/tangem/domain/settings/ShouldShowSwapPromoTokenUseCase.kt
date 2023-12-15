package com.tangem.domain.settings

import com.tangem.domain.settings.repositories.SwapPromoRepository
import kotlinx.coroutines.flow.Flow

class ShouldShowSwapPromoTokenUseCase(private val swapPromoRepository: SwapPromoRepository) {

    operator fun invoke(userWalletId: String, currencyId: String): Flow<Boolean> =
        swapPromoRepository.isReadyToShowToken(userWalletId, currencyId)

    suspend fun neverToShow(userWalletId: String, currencyId: String) =
        swapPromoRepository.setNeverToShowToken(userWalletId, currencyId)
}