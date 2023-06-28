package com.tangem.feature.learn2earn.domain.models

import com.tangem.datasource.api.promotion.models.PromotionInfoResponse

/**
[REDACTED_AUTHOR]
 */
data class Promotion(
    val info: PromotionInfo?,
    val error: PromotionError?,
) {

    @Throws(NullPointerException::class)
    fun getPromotionInfo(): PromotionInfo = info!!

    fun isError(): Boolean = error != null

    data class PromotionInfo(
        val newCard: PromotionInfoResponse.Data,
        val oldCard: PromotionInfoResponse.Data,
        val awardPaymentToken: PromotionInfoResponse.TokenData,
    )
}