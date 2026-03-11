package com.tangem.features.hotwallet

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultHotWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : HotWalletFeatureToggles {

    override val isWalletCreationRestrictionEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "HOT_WALLET_CREATION_RESTRICTION_ENABLED")
}