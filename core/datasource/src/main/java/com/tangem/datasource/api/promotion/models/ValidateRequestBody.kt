package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
* [REDACTED_AUTHOR]
 */
data class ValidateRequestBody(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "programName") val programName: String,
)
