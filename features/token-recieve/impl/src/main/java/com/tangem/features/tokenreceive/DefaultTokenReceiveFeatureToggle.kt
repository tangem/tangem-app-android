package com.tangem.features.tokenreceive

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTokenReceiveFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokenReceiveFeatureToggle {

    override val isNewTokenReceiveEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("NEW_TOKEN_RECEIVE_ENABLED")
}