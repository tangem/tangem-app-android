package com.tangem.features.forceupdate.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.forceupdate.ForceUpdateFeatureToggles

internal class DefaultForceUpdateFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : ForceUpdateFeatureToggles {

    override val isForceUpdateEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1322_FORCE_UPDATE_ENABLED)
}