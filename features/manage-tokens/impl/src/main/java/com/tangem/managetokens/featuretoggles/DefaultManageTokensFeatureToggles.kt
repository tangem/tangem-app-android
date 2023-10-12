package com.tangem.managetokens.featuretoggles

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager
import com.tangem.features.managetokens.featuretoggles.ManageTokensFeatureToggles

internal class DefaultManageTokensFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : ManageTokensFeatureToggles {
    override val isRedesignedScreenEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "REDESIGNED_MANAGE_TOKENS_SCREEN_ENABLED")
}