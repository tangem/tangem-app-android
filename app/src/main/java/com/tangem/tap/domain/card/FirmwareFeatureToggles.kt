package com.tangem.tap.domain.card

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager

/**
 * New firmware support feature toggles
 */
class FirmwareFeatureToggles internal constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) {

    val isNewFirmwareSupportEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.TWI_1524_NEW_FIRMWARE_SUPPORT)
}