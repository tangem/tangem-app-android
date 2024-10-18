package com.tangem.tap.domain.scanCard

import com.tangem.core.toggle.feature.FeatureTogglesManager

/**
 * Add custom token feature toggles
 *
 * @author Andrew Khokhlov on 04/04/2023
 */
class CardScanningFeatureToggles internal constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isNewCardScanningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NEW_CARD_SCANNING_ENABLED")
}
