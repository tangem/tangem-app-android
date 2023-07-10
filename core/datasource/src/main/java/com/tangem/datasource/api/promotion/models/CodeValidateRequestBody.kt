package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
* [REDACTED_AUTHOR]
 */
data class CodeValidateRequestBody(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "code") val code: String?,
)
