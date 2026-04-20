package com.tangem.features.hotwallet

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultHotWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : HotWalletFeatureToggles {

    override val isAssetsDiscoveryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.ASSETS_DISCOVERY_ENABLED)
}