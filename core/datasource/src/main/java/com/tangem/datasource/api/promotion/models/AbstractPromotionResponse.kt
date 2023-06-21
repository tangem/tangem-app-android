package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
 * @author Anton Zhilenkov on 05.06.2023.
 */
abstract class AbstractPromotionResponse {

    abstract val error: Error?

    fun isError(): Boolean = error != null

    data class Error(
        @Json(name = "code") val code: Int,
        @Json(name = "description") val description: String,
    )
}
