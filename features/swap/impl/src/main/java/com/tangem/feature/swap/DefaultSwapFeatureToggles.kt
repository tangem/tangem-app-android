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

    override val isTronGaslessSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1803_TRON_GASLESS_SWAP_ENABLED,
        )

    override val isTronGaslessDexSwapEnabled: Boolean
        get() = isTronGaslessSwapEnabled && featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1850_TRON_GASLESS_DEX_SWAP_ENABLED,
        )

    override val isCustomSlippageEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1674_SWAP_CUSTOM_SLIPPAGE_ENABLED,
        )

    override val isOverallBestPriceEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1876_KYC_PRESCREENING_CEX_ENABLED,
        )
}