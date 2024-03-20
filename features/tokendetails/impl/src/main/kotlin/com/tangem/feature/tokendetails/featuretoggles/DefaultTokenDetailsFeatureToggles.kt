package com.tangem.feature.tokendetails.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles

internal class DefaultTokenDetailsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokenDetailsFeatureToggles {
    override fun isGenerateXPubEnabled() = featureTogglesManager.isFeatureEnabled(name = "GENERATE_XPUB_ENABLED")
}