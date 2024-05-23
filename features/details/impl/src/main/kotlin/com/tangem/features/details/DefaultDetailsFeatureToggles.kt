package com.tangem.features.details

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultDetailsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DetailsFeatureToggles {

    override val isRedisignEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("DETAILS_REDISIGN_ENABLED")
}
