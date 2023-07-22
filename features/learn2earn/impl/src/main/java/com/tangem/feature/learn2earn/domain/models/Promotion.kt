package com.tangem.feature.learn2earn.domain.models

import com.tangem.datasource.api.promotion.models.PromotionInfoResponse

/**
 * @author Anton Zhilenkov on 16.06.2023.
 */
internal data class Promotion(
    val info: PromotionInfo?,
    val error: PromotionError?,
) {

    @Throws(NullPointerException::class)
    fun getPromotionInfo(): PromotionInfo = info!!

    data class PromotionInfo(
        val newCard: PromotionInfoResponse.Data,
        val oldCard: PromotionInfoResponse.Data,
        val awardPaymentToken: PromotionInfoResponse.TokenData,
    )
}
