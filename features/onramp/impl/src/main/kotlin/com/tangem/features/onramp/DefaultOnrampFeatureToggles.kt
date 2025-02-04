package com.tangem.features.onramp

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultOnrampFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnrampFeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "ONRAMP_ENABLED")

    override val isHotTokensEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "ONRAMP_HOT_TOKENS_ENABLED")
}
