package com.tangem.blockchainsdk.featuretoggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultBlockchainSDKFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : BlockchainSDKFeatureToggles {

    override val isEthereumEIP1559Enabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "IS_ETHEREUM_EIP_1559_ENABLED")
}