package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EarnListResponse(
    @Json(name = "meta") val meta: MetaEarnListResponse,
    @Json(name = "items") val items: List<EarnResponse>,
)

@JsonClass(generateAdapter = true)
data class EarnResponse(
    @Json(name = "apy") val apy: String,
    @Json(name = "networkId") val networkId: String,
    @Json(name = "rewardType") val rewardType: String,
    @Json(name = "type") val type: String,
    @Json(name = "token") val token: EarnTokenResponse,
)

@JsonClass(generateAdapter = true)
data class EarnTokenResponse(
    @Json(name = "id") val id: String,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "name") val name: String,
    @Json(name = "address") val address: String,
    @Json(name = "decimalCount") val decimalCount: Int? = null,
)

@JsonClass(generateAdapter = true)
data class MetaEarnListResponse(
    @Json(name = "page") val page: Int,
    @Json(name = "limit") val limit: Int,
)