package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import com.tangem.features.swap.SwapFeatureToggles

class GetSwapUiModeUseCase(
    private val swapFeatureToggles: SwapFeatureToggles,
    private val swapRepository: SwapRepository,
) {

    suspend operator fun invoke(): SwapUIMode {
        if (!swapFeatureToggles.isSwapAbEnabled) return SwapUIMode.Detailed
        // TODO: take default from Amplitude (true -> Detailed, false -> Simple).
        //  Until then default is Detailed.
        return swapRepository.getStoredSwapUiMode() ?: SwapUIMode.Detailed
    }
}