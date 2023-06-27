package com.tangem.feature.learn2earn.data.models

import com.tangem.datasource.api.promotion.models.PromotionInfoResponse

/**
[REDACTED_AUTHOR]
 */
internal fun PromotionInfoResponse.getData(promoCode: String?): PromotionInfoResponse.Data? {
    return if (promoCode == null) {
        newCard
    } else {
        oldCard
    }
}