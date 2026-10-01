package com.tangem.features.polymarket.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import javax.inject.Inject

internal class DefaultPolymarketFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : PolymarketFeatureToggles {
    override val isPolymarketEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_16204_POLYMARKET_ENABLED)
}