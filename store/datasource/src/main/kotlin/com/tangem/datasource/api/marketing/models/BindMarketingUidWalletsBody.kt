package com.tangem.datasource.api.marketing.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BindMarketingUidWalletsBody(
    @Json(name = "walletIds") val walletIds: List<String>,
)