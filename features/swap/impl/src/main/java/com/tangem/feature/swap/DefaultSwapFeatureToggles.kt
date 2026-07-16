package com.tangem.feature.swap

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles
import javax.inject.Inject

internal class DefaultSwapFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {

    override val isYieldSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1326_YIELD_MODE_SWAP_ENABLED,
        )

    override val isHighFeeWarningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1367_HIGH_FEE_WARNING_ENABLED,
        )

    override val isTronDexSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16080_TRON_DEX_SWAP_ENABLED,
        )
}