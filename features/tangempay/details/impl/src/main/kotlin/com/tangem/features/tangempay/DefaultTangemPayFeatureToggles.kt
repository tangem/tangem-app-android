package com.tangem.features.tangempay

import com.tangem.core.configtoggle.feature.FeatureTogglesManager

internal class DefaultTangemPayFeatureToggles(
    private val featureTogglesManager: FeatureTogglesManager,
) : TangemPayFeatureToggles {
    override val isTangemPayAccountsRefactorEnabled
        get() = featureTogglesManager.isFeatureEnabled("TANGEM_PAY_ACCOUNTS_REFACTOR_ENABLED")
}