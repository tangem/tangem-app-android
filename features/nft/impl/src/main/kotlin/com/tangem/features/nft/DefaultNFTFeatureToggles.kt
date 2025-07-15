package com.tangem.features.nft

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultNFTFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : NFTFeatureToggles {

    override val isNFTMediaContentEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NFT_MEDIA_CONTENT_ENABLED")
}