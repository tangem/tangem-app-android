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
    fun getInfo(): PromotionInfo = info!!

    fun isError(): Boolean = error != null

    fun isUnreachable(): Boolean = error == PromotionError.NetworkUnreachable

    data class PromotionInfo(
        val status: PromotionInfoResponse.Status,
        val awardForNewCard: Float,
        val awardForOldCard: Float,
        val awardPaymentToken: PromotionInfoResponse.TokenInfo,
    ) {
        companion object
    }

    companion object
}