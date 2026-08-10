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
            toggle = FeatureToggles.AND_16636_YIELD_DEX_TRANSFER_ENABLED,
        )

    override val isTronDexSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16080_TRON_DEX_SWAP_ENABLED,
        )

    override val isChooseTokenPulseEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16357_CHOOSE_TOKEN_PULSE_ANIMATION,
        )

    override val isHideZeroBalanceSourceEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16358_HIDE_ZERO_BALANCE_SWAP_SOURCE,
        )

    override val isSwapDeeplinkEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_16522_SWAP_DEEPLINK_ENABLED,
        )
}