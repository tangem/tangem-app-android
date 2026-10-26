package com.tangem.domain.yield.supply.models.incentives

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Merkl incentive overlay of a single yield market.
 *
 * @property rewardsApy gross rewards APY, before the service fee, in percent
 * @property totalApyEst estimated total APY (base APY + gross rewards), in percent
 */
@Serializable
data class YieldIncentiveCampaign(
    val hasActiveCampaign: Boolean,
    val rewardsApy: SerializedBigDecimal,
    val totalApyEst: SerializedBigDecimal,
)