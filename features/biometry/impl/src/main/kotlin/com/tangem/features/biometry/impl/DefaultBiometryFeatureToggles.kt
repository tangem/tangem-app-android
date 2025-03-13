package com.tangem.features.biometry.impl

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.biometry.BiometryFeatureToggles
import javax.inject.Inject

internal class DefaultBiometryFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : BiometryFeatureToggles {
    override val isAskForBiometryEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("ASK_BIOMETRY_REFACTORING_ENABLED")
}