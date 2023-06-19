package com.tangem.datasource.api.promotion.models

import com.squareup.moshi.Json

/**
 * @author Anton Zhilenkov on 05.06.2023.
 */
data class CodeAwardRequestBody(
    @Json(name = "walletId") val walletId: String,
    @Json(name = "address") val address: String,
    @Json(name = "code") val code: String?,
)
