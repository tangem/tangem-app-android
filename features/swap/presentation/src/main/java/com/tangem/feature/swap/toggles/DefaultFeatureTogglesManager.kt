package com.tangem.feature.swap.toggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

/** Feature toggles manager implementation of "swap" feature */
class DefaultFeatureTogglesManager(
    private val featureTogglesManager: FeatureTogglesManager,
) : InnerSwapFeatureTogglesManager {

    override val isOptimismSwapEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "OPTIMISM_SWAP_FEATURE_ENABLED")
}
