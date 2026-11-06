package com.tangem.domain.models.yield.supply

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.currency.balance.contributionOrNull

/**
 * Freshness of the Yield Mode state. It rides on [YieldSupplyContribution] rather than on a dedicated
 * [CryptoCurrencyStatus.Sources] field, as [CryptoCurrencyStatus.Sources.contributionSources] replaces those.
 * `null` when the currency has no Yield Mode state.
 */
val CryptoCurrencyStatus.Value.yieldSupplySource: StatusSource?
    get() = contributionOrNull<YieldSupplyContribution>()?.source