package com.tangem.features.markets

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultMarketsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : MarketsFeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("MARKETS_ENABLED")
}