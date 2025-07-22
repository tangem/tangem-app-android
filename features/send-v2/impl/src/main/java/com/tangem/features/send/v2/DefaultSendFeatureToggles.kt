package com.tangem.features.send.v2

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.send.v2.api.SendFeatureToggles

internal class DefaultSendFeatureToggles(
    private val featureToggles: FeatureTogglesManager,
) : SendFeatureToggles {
    override val isSendRedesignEnabled: Boolean
        get() = featureToggles.isFeatureEnabled("SEND_REDESIGN_ENABLED")
}
