package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenWithYieldDTO(
    @Json(name = "token") val token: TokenDTO,
    @Json(name = "availableYields") val availableYieldIds: List<String>,
)