package com.tangem.data.staking.toggles

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.staking.toggles.StakingFeatureToggles

internal class DefaultStakingFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : StakingFeatureToggles {

    override val isTonStakingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "STAKING_TON_ENABLED")
}