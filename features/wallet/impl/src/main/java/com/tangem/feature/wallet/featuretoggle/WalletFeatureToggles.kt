package com.tangem.feature.wallet.featuretoggle

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class WalletFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isTokenListLceFlowEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("TOKEN_LIST_LCE_ENABLED")
}
