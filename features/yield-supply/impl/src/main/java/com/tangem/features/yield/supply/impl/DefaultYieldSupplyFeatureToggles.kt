package com.tangem.features.yield.supply.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import javax.inject.Inject

internal class DefaultYieldSupplyFeatureToggles @Inject constructor(
    featureTogglesManager: FeatureTogglesManager,
) : YieldSupplyFeatureToggles {

    override val isYieldPromoEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.AND_15154_YIELD_PROMO_ENABLED,
    )
}