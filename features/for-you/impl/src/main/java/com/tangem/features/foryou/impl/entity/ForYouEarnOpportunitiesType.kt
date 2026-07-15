package com.tangem.features.foryou.impl.entity

import com.tangem.domain.staking.model.StakingIntegrationID

/**
 * The earn product behind an earn-opportunities row. Resolved once per token when the row is built
 * and carried through the click callback, so the model knows which earn screen to open
 * ([com.tangem.common.routing.AppRoute.YieldSupplyEntry] or [com.tangem.common.routing.AppRoute.Staking])
 * without re-resolving availability.
 */
internal sealed interface ForYouEarnOpportunitiesType {

    /**
     * Yield supply (yield module) opportunity.
     *
     * @property apy rate in percent as received from the backend (e.g. "5.5" = 5.5%), passed as-is
     * to the yield-supply entry route
     */
    data class YieldSupply(
        val apy: String,
    ) : ForYouEarnOpportunitiesType

    /**
     * Staking opportunity.
     *
     * @property integrationID staking integration to open the staking screen with
     */
    data class Staking(
        val integrationID: StakingIntegrationID,
    ) : ForYouEarnOpportunitiesType
}