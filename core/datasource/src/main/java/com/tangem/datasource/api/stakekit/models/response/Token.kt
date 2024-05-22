package com.tangem.datasource.api.stakekit.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Token(
    @Json(name = "name") val name: String,
    @Json(name = "network") val network: NetworkType,
    @Json(name = "symbol") val symbol: String,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "address") val address: String?,
    @Json(name = "coinGeckoId") val coinGeckoId: String?,
    @Json(name = "logoURI") val logoURI: String?,
    @Json(name = "isPoints") val isPoints: Boolean,
)
