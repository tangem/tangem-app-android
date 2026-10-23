package com.tangem.feature.usedesk

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles

internal class DefaultUseDeskFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : UseDeskFeatureToggles {

    override val isSupportChatDeepLinkEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1760_SUPPORT_CHAT_DEEPLINK_ENABLED)
}