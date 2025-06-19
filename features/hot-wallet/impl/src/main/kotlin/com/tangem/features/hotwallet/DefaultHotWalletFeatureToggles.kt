package com.tangem.features.hotwallet

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultHotWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : HotWalletFeatureToggles {
    override val isHotWalletEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "HOT_WALLET_ENABLED")
}