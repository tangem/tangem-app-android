package com.tangem.domain.yield.supply.models.incentives

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/**
 * Merkl incentive overlay of a single yield market.
 *
 * @property rewardsApr net rewards APR after the service fee, in percent
 * @property totalApyEst estimated total APY (base APY + net rewards), in percent
 */
@Serializable
data class YieldIncentiveCampaign(
    val hasActiveCampaign: Boolean,
    val rewardsApr: SerializedBigDecimal,
    val totalApyEst: SerializedBigDecimal,
)