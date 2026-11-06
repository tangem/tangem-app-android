package com.tangem.features.swap.v2.impl.featuretoggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.v2.api.SwapV2FeatureToggles
import javax.inject.Inject

internal class DefaultSwapV2FeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SwapV2FeatureToggles {

    override val isHideRejectedCexProviderEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1876_KYC_PRESCREENING_CEX_ENABLED,
        )
}