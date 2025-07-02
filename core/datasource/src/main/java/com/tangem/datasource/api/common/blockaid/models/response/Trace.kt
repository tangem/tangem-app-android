package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Trace(
    @Json(name = "exposed") val exposed: Exposed?,
    @Json(name = "asset") val asset: NftAsset,
)

@JsonClass(generateAdapter = true)
data class Exposed(
    @Json(name = "token_id") val tokenId: String?,
    @Json(name = "logo_url") val logoUrl: String?,
)

@JsonClass(generateAdapter = true)
data class NftAsset(
    @Json(name = "name") val name: String,
)