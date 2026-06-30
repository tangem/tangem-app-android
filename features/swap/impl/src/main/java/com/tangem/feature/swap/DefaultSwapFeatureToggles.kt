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

    override val isSwapSwitchToTransferEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15207_SWAP_SWITCH_TO_TRANSFER_ENABLED,
        )

    override val isSwapIntegratedApproveEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15120_SWAP_INTEGRATED_APPROVE,
        )

    override val isSwapAbEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.SWAP_AB_ENABLED,
        )

    override val isSwapProviderFilterEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15009_SWAP_PROVIDER_FILTER_ENABLED,
        )

    override val isSwapRateExperienceEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15103_SWAP_RATE_EXPERIENCE_ENABLED,
        )

    override val isSwapPredefinedButtonsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15122_SWAP_PREDEFINED_BUTTONS_ENABLED,
        )
    override val isExpressShareButtonEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15489_EXPRESS_SHARE_BUTTON_ENABLED,
        )

    override val isSwapBestDexRateEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.AND_15715_SWAP_BEST_DEX_RATE_ENABLED,
        ) && isSwapIntegratedApproveEnabled

    override val isHighFeeWarningEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1367_HIGH_FEE_WARNING_ENABLED,
        )
}