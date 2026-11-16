package com.tangem.features.earn.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.earn.api.EarnFeatureToggles

internal class DefaultEarnFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : EarnFeatureToggles {

    override val isEarnEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1676_EARN_ENABLED)
}