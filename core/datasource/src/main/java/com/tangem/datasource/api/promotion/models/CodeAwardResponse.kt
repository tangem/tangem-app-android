package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
[REDACTED_AUTHOR]
 */
data class CodeAwardResponse(
    @Json(name = "status") val status: Boolean?,
    @Json(name = "error") override val error: Error? = null,
) : AbstractPromotionResponse()