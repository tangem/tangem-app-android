package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
* [REDACTED_AUTHOR]
 */
data class AwardResponse(
    @Json(name = "status") val status: Boolean?,
    @Json(name = "error") override val error: Error? = null,
) : AbstractPromotionResponse()
