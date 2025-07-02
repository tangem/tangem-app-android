package com.tangem.common.routing

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

class RoutingFeatureToggle(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isDeepLinkNavigationEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "DEEPLINK_NAVIGATION_ENABLED")
}