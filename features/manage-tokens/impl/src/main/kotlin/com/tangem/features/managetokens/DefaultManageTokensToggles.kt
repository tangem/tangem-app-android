package com.tangem.features.managetokens

import com.tangem.core.featuretoggle.manager.FeatureTogglesManager

internal class DefaultManageTokensToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : ManageTokensToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled("NEW_MANAGE_TOKENS")
}