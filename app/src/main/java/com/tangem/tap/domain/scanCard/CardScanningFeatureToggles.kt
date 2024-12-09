package com.tangem.tap.domain.scanCard

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

/**
 * Add custom token feature toggles
 *
[REDACTED_AUTHOR]
 */
class CardScanningFeatureToggles internal constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isNewCardScanningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "NEW_CARD_SCANNING_ENABLED")
}