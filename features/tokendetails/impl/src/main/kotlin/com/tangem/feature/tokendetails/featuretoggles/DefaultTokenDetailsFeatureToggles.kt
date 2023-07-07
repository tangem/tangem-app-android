package com.tangem.feature.tokendetails.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.tokendetails.featuretoggles.TokenDetailsFeatureToggles

private const val REDESIGNED_TOKEN_DETAIL_SCREEN_ENABLED_KEY = "REDESIGNED_TOKEN_DETAIL_SCREEN_ENABLED"

internal class DefaultTokenDetailsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokenDetailsFeatureToggles {

    override val isRedesignedScreenEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = REDESIGNED_TOKEN_DETAIL_SCREEN_ENABLED_KEY)
}
