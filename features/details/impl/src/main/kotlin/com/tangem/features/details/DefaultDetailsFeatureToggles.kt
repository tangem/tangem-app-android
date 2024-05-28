package com.tangem.features.details

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultDetailsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DetailsFeatureToggles {

    override val isRedesignEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("DETAILS_REDESIGN_ENABLED")
}