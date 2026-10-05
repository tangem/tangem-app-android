package com.tangem.tap.routing.utils

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal interface DeepLinkFeatureToggles {

    /** Whether deep links are dispatched only to the handlers contributed by features, instead of the legacy routing. */
    val isHandlerRegistryEnabled: Boolean
}

internal class DefaultDeepLinkFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DeepLinkFeatureToggles {

    override val isHandlerRegistryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18183_DEEPLINK_HANDLER_REGISTRY_ENABLED)
}