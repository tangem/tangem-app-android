package com.tangem.tap.routing.utils

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal interface DeepLinkFeatureToggles {
    val isQrScanDeepLinksEnabled: Boolean
}

internal class DefaultDeepLinkFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : DeepLinkFeatureToggles {

    override val isQrScanDeepLinksEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1690_QR_SCAN_DEEPLINKS_ENABLED)
}