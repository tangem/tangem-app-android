package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
[REDACTED_AUTHOR]
 */
data class ValidateResponse(
    @Json(name = "valid") val valid: Boolean?,
    @Json(name = "error") override val error: Error? = null,
) : AbstractPromotionResponse()