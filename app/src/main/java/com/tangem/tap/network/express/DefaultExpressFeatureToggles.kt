package com.tangem.tap.network.express

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.grow.datasource.express.ExpressFeatureToggles

internal class DefaultExpressFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : ExpressFeatureToggles {

    override val isExpressV2Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1906_EXPRESS_V2_ENABLED)
}