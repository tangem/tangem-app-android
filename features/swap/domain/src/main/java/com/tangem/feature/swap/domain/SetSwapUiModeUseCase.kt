package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.SwapUIMode

class SetSwapUiModeUseCase(
    private val swapRepository: SwapRepository,
) {

    suspend operator fun invoke(mode: SwapUIMode) {
        swapRepository.storeSwapUiMode(mode)
    }
}