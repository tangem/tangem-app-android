package com.tangem.tap.routing.utils

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal interface DeepLinkFeatureToggles {

    /** Whether deep links are dispatched only to the handlers contributed by features, instead of the legacy routing. */
    val isHandlerRegistryEnabled: Boolean

    val isQrScanDeepLinksEnabled: Boolean
}

internal class DefaultDeepLinkFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DeepLinkFeatureToggles {

    override val isHandlerRegistryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.AND_18183_DEEPLINK_HANDLER_REGISTRY_ENABLED)

    override val isQrScanDeepLinksEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1690_QR_SCAN_DEEPLINKS_ENABLED)
}