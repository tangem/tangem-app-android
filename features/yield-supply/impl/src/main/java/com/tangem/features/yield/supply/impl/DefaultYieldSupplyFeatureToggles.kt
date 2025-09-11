package com.tangem.features.yield.supply.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles

internal class DefaultYieldSupplyFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : YieldSupplyFeatureToggles {
    override val isYieldSupplyFeatureEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("YIELD_SUPPLY_FEATURE_ENABLED")
}