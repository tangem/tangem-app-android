package com.tangem.feature.swap

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles
import javax.inject.Inject

internal class DefaultSwapFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {

    override val isYieldDexTransferEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1326_YIELD_DEX_TRANSFER_ENABLED,
        )

    override val isChooseTokenPulseEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16357_CHOOSE_TOKEN_PULSE_ANIMATION,
        )

    override val isExpressCategoriesGeoBlockingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1643_EXPRESS_CATEGORIES_GEO_BLOCKING_ENABLED,
        )

    override val isTronGaslessSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1803_TRON_GASLESS_SWAP_ENABLED,
        )
}