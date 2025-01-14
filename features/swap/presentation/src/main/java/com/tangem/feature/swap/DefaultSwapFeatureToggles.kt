package com.tangem.feature.swap

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles

internal class DefaultSwapFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : SwapFeatureToggles {
    override val isPromoStoriesEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("SWAP_STORIES_ENABLED")
}