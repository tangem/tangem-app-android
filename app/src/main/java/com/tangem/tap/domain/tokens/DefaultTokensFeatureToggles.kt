package com.tangem.tap.domain.tokens

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.tokens.TokensFeatureToggles

internal class DefaultTokensFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokensFeatureToggles {

    override val isDynamicAddressesEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.DYNAMIC_ADDRESSES_ENABLED)
}