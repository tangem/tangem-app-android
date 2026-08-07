package com.tangem.feature.swap

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles
import javax.inject.Inject

internal class DefaultSwapFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {

    override val isTronDexSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16080_TRON_DEX_SWAP_ENABLED,
        )

    override val isChooseTokenPulseEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16357_CHOOSE_TOKEN_PULSE_ANIMATION,
        )
}