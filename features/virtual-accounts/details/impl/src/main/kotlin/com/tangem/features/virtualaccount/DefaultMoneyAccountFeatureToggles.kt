package com.tangem.features.virtualaccount

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import javax.inject.Inject

internal class DefaultMoneyAccountFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : MoneyAccountFeatureToggles {
    override val isMoneyAccountsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.AND_15325_MONEY_ACCOUNTS_ENABLED)
}