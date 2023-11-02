package com.tangem.feature.swap.models.response

import com.squareup.moshi.Json

data class ExchangeProvider(
    @Json(name = "id")
    val id: Int,

    @Json(name = "name")
    val name: String,

    @Json(name = "id")
    val type: ExchangeProviderType,

    @Json(name = "imageLarge")
    val imageLargeUrl: Int,

    @Json(name = "imageSmall")
    val imageSmallUrl: Int,
)

enum class ExchangeProviderType {
    @Json(name = "dex")
    DEX,

    @Json(name = "cex")
    CEX,
}