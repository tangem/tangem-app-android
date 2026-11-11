package com.tangem.features.allservices.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.allservices.api.AllServicesFeatureToggles

internal class DefaultAllServicesFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : AllServicesFeatureToggles {

    override val isAllServicesEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1720_ALL_SERVICES_ENABLED)
}