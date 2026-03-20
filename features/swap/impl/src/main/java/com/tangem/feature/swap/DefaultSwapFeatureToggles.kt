package com.tangem.feature.swap

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles

internal class DefaultSwapFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {
    override val isMarketListFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.SWAP_MARKET_LIST_ENABLED)
}