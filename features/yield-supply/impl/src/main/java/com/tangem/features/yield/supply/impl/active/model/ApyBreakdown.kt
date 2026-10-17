package com.tangem.features.yield.supply.impl.active.model

import com.tangem.domain.yield.supply.models.YieldMarketToken
import java.math.BigDecimal

/** The two parts the incentivised APY is made of, as the About APY sheet lists them. */
internal data class ApyBreakdown(val baseApy: BigDecimal, val rewardsApr: BigDecimal)

/** Null whenever the rate has nothing to break down, which is what keeps the APY info entry hidden. */
internal fun resolveApyBreakdown(market: YieldMarketToken): ApyBreakdown? {
    val campaign = market.campaign?.takeIf { it.hasActiveCampaign } ?: return null
    if (campaign.rewardsApr <= BigDecimal.ZERO) return null

    return ApyBreakdown(baseApy = market.apy, rewardsApr = campaign.rewardsApr)
}