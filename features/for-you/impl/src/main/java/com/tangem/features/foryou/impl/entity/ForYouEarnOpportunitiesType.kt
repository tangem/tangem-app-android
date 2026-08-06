package com.tangem.features.foryou.impl.entity

import com.tangem.domain.models.earn.EarnRewardType
import com.tangem.domain.staking.model.StakingIntegrationID

/**
 * The earn product behind an earn-opportunities row. Resolved once per token when the row is built
 * and carried through the click callback, so the model knows which earn screen to open
 * ([com.tangem.common.routing.AppRoute.YieldSupplyEntry] or [com.tangem.common.routing.AppRoute.Staking])
 * without re-resolving availability.
 */
internal sealed interface ForYouEarnOpportunitiesType {

    /** Value reported in the `Type` analytics param. */
    val analyticsValue: String

    /** Whether the opportunity's rate is expressed as an APY or an APR. */
    val rewardType: EarnRewardType

    /**
     * Yield supply (yield module) opportunity.
     *
     * @property apy rate in percent as received from the backend (e.g. "5.5" = 5.5%), passed as-is
     * to the yield-supply entry route
     */
    data class YieldSupply(
        val apy: String,
        override val rewardType: EarnRewardType = EarnRewardType.APY,
    ) : ForYouEarnOpportunitiesType {
        override val analyticsValue: String = "Yield"
    }

    /**
     * Staking opportunity.
     *
     * @property integrationID staking integration to open the staking screen with
     */
    data class Staking(
        val integrationID: StakingIntegrationID,
        override val rewardType: EarnRewardType,
    ) : ForYouEarnOpportunitiesType {
        override val analyticsValue: String = "Staking"
    }
}