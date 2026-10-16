package com.tangem.features.yield.supply.api.promobottomsheet

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

/** Fixed at the moment the promo bottom sheet is shown. [apy] is in percent: 9.2 means 9.2 %. */
@Serializable
data class YieldModePromoOffer(
    val apy: SerializedBigDecimal,
    val tokensCount: Int,
    val tokenSymbol: String,
    val blockchain: String,
)