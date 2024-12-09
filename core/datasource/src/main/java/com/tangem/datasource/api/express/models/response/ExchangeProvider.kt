package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

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

    @Json(name = "termsOfUse")
    val termsOfUse: String?,

    @Json(name = "privacyPolicy")
    val privacyPolicy: String?,

    @Json(name = "recommended")
    val isRecommended: Boolean = false,

    @Json(name = "slippage")
    val slippage: BigDecimal?,
)

enum class ExchangeProviderType {
    @Json(name = "dex")
    DEX,

    @Json(name = "cex")
    CEX,

    @Json(name = "dex-bridge")
    DEX_BRIDGE,

    @Json(name = "onramp")
    ONRAMP,
}