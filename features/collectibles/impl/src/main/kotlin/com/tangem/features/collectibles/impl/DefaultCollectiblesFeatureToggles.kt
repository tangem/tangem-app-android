package com.tangem.features.collectibles.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles

internal class DefaultCollectiblesFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : CollectiblesFeatureToggles {

    override val isCollectiblesEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1640_COLLECTIBLES_PHASE_ONE_ENABLED)
}