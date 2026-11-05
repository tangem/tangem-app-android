package com.tangem.features.yield.supply.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.utils.annotations.RemoveWithToggle
import javax.inject.Inject

internal class DefaultYieldSupplyFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : YieldSupplyFeatureToggles {

    override val isYieldModePromoEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1762_YIELD_MODE_PROMO_ENABLED)

    @RemoveWithToggle("AND_19119_YIELD_MODE_SMART_GAS_ENABLED")
    override val isSmartGasEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_19119_YIELD_MODE_SMART_GAS_ENABLED)
}