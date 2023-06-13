package com.tangem.tap.features.customtoken.impl.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles

/**
 * Default implementation of CustomToken feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
[REDACTED_AUTHOR]
 */
internal class DefaultCustomTokenFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : CustomTokenFeatureToggles {

    override val isRedesignedScreenEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_CUSTOM_TOKEN_SCREEN_ENABLED")

    override val isNewCardScanningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NEW_CARD_SCANNING_ENABLED")
}