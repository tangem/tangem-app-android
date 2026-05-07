package com.tangem.feature.swap

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.features.swap.SwapFeatureToggles
import javax.inject.Inject

internal class DefaultSwapFeatureToggles @Inject constructor(
    featureTogglesManager: FeatureTogglesManager,
) : SwapFeatureToggles {

    override val isSwapSwitchToTransferEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.SWAP_SWITCH_TO_TRANSFER_ENABLED,
    )

    override val isSwapIntegratedApproveEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.SWAP_INTEGRATED_APPROVE,
    )

    override val isSwapAbEnabled: Boolean = featureTogglesManager.isFeatureEnabled(
        toggle = FeatureToggles.SWAP_AB_ENABLED,
    )
}