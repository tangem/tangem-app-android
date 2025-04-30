package com.tangem.features.nft

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultNFTFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : NFTFeatureToggles {
    override val isNFTEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NFT_ENABLED")

    override val isNFTEVMEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NFT_EVM_ENABLED")

    override val isNFTSolanaEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NFT_SOLANA_ENABLED")
}