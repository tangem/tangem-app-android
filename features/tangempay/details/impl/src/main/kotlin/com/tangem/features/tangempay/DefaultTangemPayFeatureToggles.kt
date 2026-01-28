package com.tangem.features.tangempay

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isTangemPayEnabled
        get() = featureTogglesManager.isFeatureEnabled("TANGEM_PAY_ENABLED")
    override val isEntryPointsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("TANGEM_PAY_ENTRYPOINT_ENABLED")
}