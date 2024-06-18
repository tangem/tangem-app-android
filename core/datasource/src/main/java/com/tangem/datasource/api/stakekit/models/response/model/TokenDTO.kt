package com.tangem.datasource.api.stakekit.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TokenDTO(
    @Json(name = "name")
    val name: String,
    @Json(name = "network")
    val network: NetworkTypeDTO,
    @Json(name = "symbol")
    val symbol: String,
    @Json(name = "decimals")
    val decimals: Int,
    @Json(name = "address")
    val address: String?,
    @Json(name = "coinGeckoId")
    val coinGeckoId: String?,
    @Json(name = "logoURI")
    val logoURI: String?,
    @Json(name = "isPoints")
    val isPoints: Boolean?,
)
