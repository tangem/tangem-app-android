package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssetDiff(
    @Json(name = "asset_type") val assetType: String,
    @Json(name = "asset") val asset: Asset,
    @Json(name = "in") val inTransfer: List<Transfer>? = null,
    @Json(name = "out") val outTransfer: List<Transfer>? = null,
)

@JsonClass(generateAdapter = true)
data class Asset(
    @Json(name = "chain_id") val chainId: Int? = null,
    @Json(name = "logo_url") val logoUrl: String? = null,
    @Json(name = "symbol") val symbol: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "decimals") val decimals: Int? = null,
)

@JsonClass(generateAdapter = true)
data class Transfer(
    @Json(name = "value") val value: String?,
    @Json(name = "raw_value") val rawValue: String?,
)