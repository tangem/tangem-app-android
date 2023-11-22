package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json

data class ExchangeProvider(
    @Json(name = "id")
    val id: String,

    @Json(name = "name")
    val name: String,

    @Json(name = "type")
    val type: ExchangeProviderType,

    @Json(name = "imageLarge")
    val imageLargeUrl: String,

    @Json(name = "imageSmall")
    val imageSmallUrl: String,
)

enum class ExchangeProviderType {
    @Json(name = "dex")
    DEX,

    @Json(name = "cex")
    CEX,
}
