package com.tangem.features.yieldlending.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yieldlending.api.YieldLendingFeatureToggles

internal class DefaultYieldLendingFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : YieldLendingFeatureToggles {
    override val isYieldLendingFeatureEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("YIELD_LENDING_FEATURE_ENABLED")
}