package com.tangem.feature.swap

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles

internal class DefaultSwapFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {
    override val isMarketListFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("SWAP_MARKET_LIST_ENABLED")
}