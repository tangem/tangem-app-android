package com.tangem.domain.models.currency.balance

import com.tangem.domain.models.currency.CryptoCurrencyStatus
import java.math.BigDecimal

/**
 * Sum of every extra balance folded into this currency's total, or `null` when there is none.
 *
 * Returning `null` instead of zero for "no contributions" is deliberate: call sites distinguish *"this currency
 * has nothing beyond its network amount"* — hide the balance selector, return the bare amount untouched, which
 * may itself be `null` — from *"it has a contribution that happens to be worth zero"*. Collapsing the two would
 * turn a missing amount into `0`. Use `.orZero()` where that distinction does not matter.
 */
fun CryptoCurrencyStatus.Value.totalContributionsDeltaOrNull(): BigDecimal? {
    return contributions
        .takeIf(List<BalanceContribution>::isNotEmpty)
        ?.sumOf { it.totalDeltaCryptoAmount() }
}

/**
 * First contribution of type [T], or `null` when this currency has none.
 *
 * The generic half of every Axis-2 accessor: the base module can select a contribution by type without naming any
 * concrete balance type, so accessors live in the module that owns their type instead of here.
 */
inline fun <reified T : BalanceContribution> CryptoCurrencyStatus.Value.contributionOrNull(): T? {
    return contributions.filterIsInstance<T>().firstOrNull()
}