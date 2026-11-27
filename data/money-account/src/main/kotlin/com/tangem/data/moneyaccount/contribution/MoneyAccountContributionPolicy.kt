package com.tangem.data.moneyaccount.contribution

import com.tangem.domain.moneyaccount.MoneyAccountFeatureToggles
import javax.inject.Inject

/**
 * Whether the Money account takes its [MoneyAccountContributions]: only while its Yield Mode is on
 * ([MoneyAccountFeatureToggles.isYieldModeEnabled] already requires the contribution mode).
 */
internal class MoneyAccountContributionPolicy @Inject constructor(
    private val moneyAccountFeatureToggles: MoneyAccountFeatureToggles,
) {

    val isEnabled: Boolean
        get() = moneyAccountFeatureToggles.isYieldModeEnabled
}