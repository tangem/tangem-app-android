package com.tangem.blockchainsdk.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultBlockchainSDKFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : BlockchainSDKFeatureToggles {

    override val isCardanoTokensSupportEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "CARDANO_TOKENS_SUPPORT_ENABLED")
}
