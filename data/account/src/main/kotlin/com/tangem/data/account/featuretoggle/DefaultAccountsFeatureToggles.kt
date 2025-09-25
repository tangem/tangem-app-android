package com.tangem.data.account.featuretoggle

import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles

internal class DefaultAccountsFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : AccountsFeatureToggles {

    override val isFeatureEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(name = "ACCOUNTS_FEATURE_ENABLED")
}