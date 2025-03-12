package com.tangem.features.askbiometry.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.askbiometry.AskBiometryFeatureToggles
import javax.inject.Inject

class DefaultAskBiometryFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : AskBiometryFeatureToggles {
    override val isEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("ASK_BIOMETRY_REFACTORING_ENABLED")
}