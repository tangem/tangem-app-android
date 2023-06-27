package com.tangem.feature.learn2earn.domain.models

import com.tangem.datasource.api.promotion.models.PromotionInfoResponse

/**
[REDACTED_AUTHOR]
 */
internal fun Promotion.PromotionInfo.getData(promoCode: String?): PromotionInfoResponse.Data {
    return if (promoCode == null) {
        newCard
    } else {
        oldCard
    }
}