package com.tangem.domain.yield.supply.models.incentives

import java.math.BigDecimal

/**
 * Merkl rewards of a yield module, reduced to the state the reward surfaces act on.
 *
 * [YieldReward.finalDestination] of [YieldRewardDestination.Unsupported] is dropped everywhere: such a reward cannot
 * be routed by this app version, so counting it would promise a payout the claim cannot deliver.
 *
 * @property moduleAddress module the rewards belong to, established from the incentives response and used to pin
 * the claim destination
 */
sealed interface YieldRewardsSummary {

    /** Nothing to surface — no module, no rewards, or only rewards this version cannot route. */
    data object None : YieldRewardsSummary

    /** Rewards are still accruing on Merkl's side; there is no amount to claim and no USD value to show. */
    data class PendingOnly(val pending: List<YieldReward>) : YieldRewardsSummary

    /** Claimable, but the backend recommends waiting — claiming now would cost more than it returns. */
    data class Deferred(
        val moduleAddress: String,
        val claimable: List<YieldReward>,
        val pending: List<YieldReward>,
        val totalNetUsd: BigDecimal,
    ) : YieldRewardsSummary

    /** At least one claimable reward the backend recommends claiming. */
    data class Claimable(
        val moduleAddress: String,
        val claimable: List<YieldReward>,
        val pending: List<YieldReward>,
        val totalNetUsd: BigDecimal,
    ) : YieldRewardsSummary
}