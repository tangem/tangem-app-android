package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Error body of the promotion API; [message] names the error (e.g. `Campaign not found`). */
@JsonClass(generateAdapter = true)
data class PromotionErrorResponse(
    @Json(name = "message") val message: String?,
)