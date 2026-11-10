package com.tangem.features.moneyaccount

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import javax.inject.Inject

internal class DefaultMoneyAccountFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
) : MoneyAccountFeatureToggles {
    override val isMoneyAccountsEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1525_MONEY_ACCOUNTS_ENABLED)

    override val isMxnEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1778_MONEY_ACCOUNT_MXN_ENABLED)

    override val isYieldModeEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1525_MONEY_ACCOUNT_YIELD_MODE_ENABLED,
        )
}