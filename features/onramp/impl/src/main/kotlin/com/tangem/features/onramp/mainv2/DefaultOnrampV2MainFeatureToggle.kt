package com.tangem.features.onramp.mainv2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

class DefaultOnrampV2MainFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnrampV2MainFeatureToggle {
    override val isOnrampNewMainEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("NEW_ONRAMP_MAIN_ENABLED")
}