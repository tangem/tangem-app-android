package com.tangem.features.markets

import com.tangem.core.toggle.feature.FeatureTogglesManager

internal class DefaultMarketsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : MarketsFeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("MARKETS_ENABLED")
}
