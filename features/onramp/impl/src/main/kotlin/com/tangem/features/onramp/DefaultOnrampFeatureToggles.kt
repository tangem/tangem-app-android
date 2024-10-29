package com.tangem.features.onramp

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultOnrampFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnrampFeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("ONRAMP_ENABLED")
}
