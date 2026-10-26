package com.tangem.features.yield.supply.api.promobottomsheet

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/** [apy] is in percent with two decimals: 9.20 means 9.2 %. */
@Serializable
data class YieldModePromoOffer(
    val apy: SerializedBigDecimal,
    val tokensCount: Int,
    val tokenSymbol: String,
    val blockchain: String,
)