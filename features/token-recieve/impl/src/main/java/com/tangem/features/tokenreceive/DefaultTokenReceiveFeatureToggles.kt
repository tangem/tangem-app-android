package com.tangem.features.tokenreceive

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTokenReceiveFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokenReceiveFeatureToggles {

    override val isNewReceiveScreenEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1958_ADD_FUNDS_RECEIVE_STAGE_1_ENABLED)
}