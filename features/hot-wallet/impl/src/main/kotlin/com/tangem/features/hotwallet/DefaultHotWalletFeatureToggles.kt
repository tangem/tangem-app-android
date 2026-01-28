package com.tangem.features.hotwallet

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultHotWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : HotWalletFeatureToggles {
    override val isHotWalletEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "HOT_WALLET_ENABLED")
    override val isWalletCreationRestrictionEnabled: Boolean
        get() = isHotWalletEnabled &&
            featureTogglesManager.isFeatureEnabled(name = "HOT_WALLET_CREATION_RESTRICTION_ENABLED")
    override val isHotWalletVisible: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "HOT_WALLET_VISIBLE")
}