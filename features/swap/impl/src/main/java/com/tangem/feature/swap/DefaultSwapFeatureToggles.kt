package com.tangem.feature.swap

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles
import javax.inject.Inject

internal class DefaultSwapFeatureToggles @Inject constructor(
    featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {

    override val isSwapSwitchToTransferEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.AND_15207_SWAP_SWITCH_TO_TRANSFER_ENABLED,
    )

    override val isSwapIntegratedApproveEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.SWAP_INTEGRATED_APPROVE,
    )

    override val isSwapAbEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.SWAP_AB_ENABLED,
    )

    override val isSwapProviderFilterEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.AND_15009_SWAP_PROVIDER_FILTER_ENABLED,
    )

    override val isSwapRateExperienceEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.AND_15103_SWAP_RATE_EXPERIENCE_ENABLED,
    )

    override val isSwapPredefinedButtonsEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.AND_15122_SWAP_PREDEFINED_BUTTONS_ENABLED,
    )
}