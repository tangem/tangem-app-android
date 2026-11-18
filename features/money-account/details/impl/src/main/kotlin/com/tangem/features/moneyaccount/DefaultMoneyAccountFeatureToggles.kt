package com.tangem.features.moneyaccount

import com.tangem.core.configtoggle.FeatureToggles
import com.tangem.core.configtoggle.feature.FeatureTogglesManager
import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.tokens.TokensFeatureToggles
import javax.inject.Inject

internal class DefaultMoneyAccountFeatureToggles @Inject constructor(
    private val featureTogglesManager: FeatureTogglesManager,
    private val tokensFeatureToggles: TokensFeatureToggles,
) : MoneyAccountFeatureToggles {
    override val isMxnEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(toggle = FeatureToggles.TWI_1778_MONEY_ACCOUNT_MXN_ENABLED)

    /** The Money account takes yield only as a balance contribution, so it needs the contribution mode too. */
    override val isYieldModeEnabled: Boolean
        get() = featureTogglesManager.isFeatureEnabled(
            toggle = FeatureToggles.TWI_1525_MONEY_ACCOUNT_YIELD_MODE_ENABLED,
        ) && tokensFeatureToggles.isYieldContributionEnabled
}