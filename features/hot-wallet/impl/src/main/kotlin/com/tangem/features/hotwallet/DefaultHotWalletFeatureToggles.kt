package com.tangem.features.hotwallet

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultHotWalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : HotWalletFeatureToggles {

    override val isWalletCreationRestrictionEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.HOT_WALLET_CREATION_RESTRICTION_ENABLED)

    override val isTokenSyncEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TOKEN_SYNC_ENABLED)
}