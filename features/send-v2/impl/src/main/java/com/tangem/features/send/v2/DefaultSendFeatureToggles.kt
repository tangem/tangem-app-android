package com.tangem.features.send.v2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.send.v2.api.SendFeatureToggles

class DefaultSendFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : SendFeatureToggles {
    override val isSendV2Enabled: Boolean
        get() = featureToggles.isFeatureEnabled("SEND_V2_ENABLED")
}