package com.tangem.features.swap.v2.impl

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.v2.api.SwapFeatureToggles
import javax.inject.Inject

internal class DefaultSwapFeatureToggles @Inject constructor(
    private val featureToggles: FeatureTogglesManager,
) : SwapFeatureToggles {
    override val isHighFeeWarningEnabled: Boolean =
        featureToggles.isFeatureEnabled(FeatureToggles.TWI_1367_HIGH_FEE_WARNING_ENABLED)
}