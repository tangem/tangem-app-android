package com.tangem.data.moneyaccount.contribution

import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import com.tangem.domain.tokens.TokensFeatureToggles
import javax.inject.Inject

/** Whether the Money account takes its [MoneyAccountContributions]: only in contribution mode. */
internal class MoneyAccountContributionPolicy @Inject constructor(
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    val isEnabled: Boolean
        get() = moneyAccountFeatureToggles.isYieldModeEnabled && tokensFeatureToggles.isYieldContributionEnabled
}