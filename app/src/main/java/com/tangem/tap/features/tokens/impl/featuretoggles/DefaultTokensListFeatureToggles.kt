package com.tangem.tap.features.tokens.impl.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.tokens.api.featuretoggles.TokensListFeatureToggles

/**
 * Default implementation of TokensList feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
* [REDACTED_AUTHOR]
 */
internal class DefaultTokensListFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TokensListFeatureToggles {

    override val isRedesignedScreenEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_TOKEN_LIST_SCREEN_ENABLED")
}
