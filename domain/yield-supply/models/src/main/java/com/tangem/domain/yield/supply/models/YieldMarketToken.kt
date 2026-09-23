package com.tangem.domain.yield.supply.models

import com.tangem.domain.models.serialization.SerializedBigDecimal
import com.tangem.domain.yield.supply.models.incentives.YieldIncentiveCampaign
import kotlinx.serialization.Serializable

/**
 * Domain model representing a token entry in the Yield Markets list.
 */
@Serializable
data class YieldMarketToken(
    val tokenAddress: String,
    val chainId: Int,
    val apy: SerializedBigDecimal,
    val isActive: Boolean,
    val maxFeeNative: SerializedBigDecimal,
    val maxFeeUSD: SerializedBigDecimal,
    val backendId: String? = null,
    val campaign: YieldIncentiveCampaign? = null,
) {

    val yieldSupplyKey: String
        get() = "${backendId}_$tokenAddress"
}