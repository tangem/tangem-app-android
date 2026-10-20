package com.tangem.features.yield.supply.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import javax.inject.Inject

internal class DefaultYieldSupplyFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : YieldSupplyFeatureToggles {

    override val isYieldModePromoEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1762_YIELD_MODE_PROMO_ENABLED)
}