package com.tangem.tap.features.details.ui.resetcard.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultResetCardFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : ResetCardFeatureToggles {

    override val isFullResetEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "FULL_RESET_ENABLED")
}