package com.tangem.features.swap.v2.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.v2.api.SwapFeatureToggles

internal class DefaultSwapFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : SwapFeatureToggles {
    override val isSwapRedesignEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("SWAP_REDESIGN_ENABLED")
}