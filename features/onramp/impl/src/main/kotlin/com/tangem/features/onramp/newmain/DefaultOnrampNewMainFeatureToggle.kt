package com.tangem.features.onramp.newmain

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

class DefaultOnrampNewMainFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) : OnrampNewMainFeatureToggle {
    override val isOnrampNewMainEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("NEW_ONRAMP_RECEIVE_ENABLED")
}