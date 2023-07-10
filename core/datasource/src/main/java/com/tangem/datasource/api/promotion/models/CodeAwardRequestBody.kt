package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
* [REDACTED_AUTHOR]
 */
data class CodeAwardRequestBody(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "address") val address: String,
    @Json(name = "code") val code: String?,
)
