package com.tangem.features.staking.impl.featuretoggles

import com.tangem.core.toggle.feature.FeatureTogglesManager
import com.tangem.features.staking.api.featuretoggles.StakingFeatureToggles

/**
 * Default implementation of staking feature toggles
 *
 * @property featureTogglesManager manager for getting information about the availability of feature toggles
 */
internal class DefaultStakingFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : StakingFeatureToggles {

    override val isStakingEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "STAKING_ENABLED")
}
