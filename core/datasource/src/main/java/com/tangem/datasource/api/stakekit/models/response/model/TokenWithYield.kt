package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenWithYield(
    @Json(name = "token") val token: Token,
    @Json(name = "availableYields") val availableYieldIds: List<String>,
)