package com.tangem.feature.swap.domain

import com.tangem.core.abtests.manager.ABTestsManager
import com.tangem.feature.swap.domain.api.SwapRepository
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.utils.logging.TangemLogger

class GetSwapUiModeUseCase(
    private val swapFeatureToggles: SwapFeatureToggles,
    private val swapRepository: SwapRepository,
    private val abTestsManager: ABTestsManager,
) {

    suspend operator fun invoke(): SwapUIMode {
        if (!swapFeatureToggles.isSwapAbEnabled) return SwapUIMode.Detailed
        swapRepository.getStoredSwapUiMode()?.let { return it }
        val variant = abTestsManager.getValue(KEY_SWAP_FORM_VARIANT, SwapUIMode.Detailed.key)
        TangemLogger.d("Get $variant Swap AB variant from Amplitude as default value")
        return SwapUIMode.entries.firstOrNull { it.key.equals(variant, ignoreCase = true) }
            ?: SwapUIMode.Detailed
    }

    private companion object {
        const val KEY_SWAP_FORM_VARIANT = "swap_form_variant"
    }
}