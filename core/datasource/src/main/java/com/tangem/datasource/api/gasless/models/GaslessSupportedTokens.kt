package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GaslessSupportedTokens(
    @Json(name = "tokens") val tokens: List<GaslessTokenDTO>,
)