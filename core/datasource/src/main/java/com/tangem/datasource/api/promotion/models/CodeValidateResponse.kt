package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
[REDACTED_AUTHOR]
 */
data class CodeValidateResponse(
    @Json(name = "valid") val valid: Boolean?,
    @Json(name = "error") override val error: Error? = null,
) : AbstractPromotionResponse()