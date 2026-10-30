package com.tangem.domain.yield.supply.models.incentives

import java.math.BigDecimal

/**
 * Merkl rewards of a yield module, reduced to the state the reward surfaces act on.
 *
 * [YieldReward.finalDestination] of [YieldRewardDestination.Unsupported] never counts towards a total or a pending
 * amount: such a reward cannot be routed by this app version, so counting it would promise a payout the claim cannot
 * deliver. It is still reported separately, so the claim sheet can name what it is leaving out and the claim gate can
 * refuse a payload that pays one out.
 *
 * @property moduleAddress module the rewards belong to, established from the incentives response and used to pin
 * the claim destination
 */
sealed interface YieldRewardsSummary {

    /** Nothing to surface — no module, no rewards, or only rewards this version cannot route. */
    data object None : YieldRewardsSummary

    /** Rewards are still accruing on Merkl's side; there is no amount to claim and no USD value to show. */
    data class PendingOnly(val pending: List<YieldReward>) : YieldRewardsSummary

    /**
     * Claimable, but the backend recommends waiting — claiming now would cost more than it returns.
     *
     * @property unsupported every reward this version cannot route, whatever its amount, excluded from [totalNetUsd]
     * @property supportedRewardTokens address of every reward the service currently reports as routable,
     * whatever its amounts — the set a claim payload is admitted against
     */
    data class Deferred(
        val moduleAddress: String,
        val claimable: List<YieldReward>,
        val pending: List<YieldReward>,
        val unsupported: List<YieldReward>,
        val supportedRewardTokens: Set<String>,
        val totalNetUsd: BigDecimal,
    ) : YieldRewardsSummary

    /**
     * At least one claimable reward the backend recommends claiming.
     *
     * @property unsupported every reward this version cannot route, whatever its amount, excluded from [totalNetUsd]
     * @property supportedRewardTokens address of every reward the service currently reports as routable,
     * whatever its amounts — the set a claim payload is admitted against
     */
    data class Claimable(
        val moduleAddress: String,
        val claimable: List<YieldReward>,
        val pending: List<YieldReward>,
        val unsupported: List<YieldReward>,
        val supportedRewardTokens: Set<String>,
        val totalNetUsd: BigDecimal,
    ) : YieldRewardsSummary
}