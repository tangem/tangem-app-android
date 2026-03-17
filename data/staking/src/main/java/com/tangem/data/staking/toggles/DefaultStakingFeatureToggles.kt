package com.tangem.data.staking.toggles

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.staking.toggles.StakingFeatureToggles

internal class DefaultStakingFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : StakingFeatureToggles {

    override val isEthStakingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(FeatureToggles.STAKING_ETH_ENABLED)
}