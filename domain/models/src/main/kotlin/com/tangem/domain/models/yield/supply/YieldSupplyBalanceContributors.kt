package com.tangem.domain.models.yield.supply

import com.tangem.domain.models.currency.balance.BalanceContributionFetcher
import com.tangem.domain.models.currency.balance.BalanceContributionProvider

/**
 * The [BalanceContributionProvider] of [YieldSupplyContribution]s, injectable on its own by a holder that takes only
 * some contribution types. The portfolio still gets it through the set of every provider.
 */
interface YieldSupplyBalanceProvider : BalanceContributionProvider

/** The [BalanceContributionFetcher] of [YieldSupplyContribution]s; see [YieldSupplyBalanceProvider]. */
interface YieldSupplyBalanceFetcher : BalanceContributionFetcher