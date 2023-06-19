package com.tangem.feature.learn2earn.domain.models

import com.tangem.datasource.api.promotion.models.AbstractPromotionResponse

/**
 * @author Anton Zhilenkov on 16.06.2023.
 */
sealed class PromotionError(val code: Int, val description: String) {
    object NetworkUnreachable : PromotionError(-1, "Network or service is unreachable")
    class Error(code: Int, description: String) : PromotionError(code, description)

    companion object {
        fun AbstractPromotionResponse.Error.toDomainError(): PromotionError = Error(code, description)
    }
}
