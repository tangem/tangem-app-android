package com.tangem.tap.core.ui

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.core.ui.HoldToConfirmButtonFeatureToggles
import javax.inject.Inject

class DefaultHoldToConfirmButtonFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : HoldToConfirmButtonFeatureToggles {
    override val isHoldToConfirmEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.HOLD_TO_CONFIRM_BUTTON_ENABLED)
}