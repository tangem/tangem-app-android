package com.tangem.domain.models.currency.balance

import com.tangem.domain.models.wallet.UserWallet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.math.BigDecimal

/**
 * Frames of these providers for [userWallet], one resolver per provider.
 *
 * Emits once every provider has emitted, and an empty list straight away when there is none.
 */
fun Collection<BalanceContributionProvider>.resolversFlow(userWallet: UserWallet): Flow<List<ContributionResolver>> {
    if (isEmpty()) return flowOf(emptyList())

    return combine(map { provider -> provider.contributions(userWallet) }) { resolvers -> resolvers.toList() }
}

/**
 * Folds what the contributions add to the spendable balance into this network amount, so `amount` and `fiatAmount`
 * keep meaning "what can be sent" wherever that balance is fetched from. Returns this amount untouched, scale included,
 * when nothing spendable is added.
 */
fun BigDecimal.withSpendableContributions(contributions: List<BalanceContribution>): BigDecimal {
    val spendableDelta = contributions.sumOf(BalanceContribution::spendableDeltaCryptoAmount)

    return if (spendableDelta.signum() == 0) this else this + spendableDelta
}