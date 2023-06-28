package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
[REDACTED_AUTHOR]
 */
abstract class AbstractPromotionResponse {

    abstract val error: Error?

    fun isError(): Boolean = error != null

    data class Error(
        @Json(name = "code") val code: Int,
        @Json(name = "message") val message: String,
    )
}