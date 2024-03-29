package com.tangem.tap.features.customtoken.impl.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.tap.features.customtoken.api.featuretoggles.CustomTokenFeatureToggles

/**
 * Default implementation of CustomToken feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 *
 * @author Andrew Khokhlov on 04/04/2023
 */
internal class DefaultCustomTokenFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : CustomTokenFeatureToggles {

    override val isNewCardScanningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NEW_CARD_SCANNING_ENABLED")
}
