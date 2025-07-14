package com.tangem.datasource.api.onramp.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampQuoteResponse(
    @Json(name = "fromCurrencyCode")
    val fromCurrencyCode: String,

    @Json(name = "toContractAddress")
    val toContractAddress: String,

    @Json(name = "toNetwork")
    val toNetwork: String,

    @Json(name = "paymentMethod")
    val paymentMethod: String,

    @Json(name = "countryCode")
    val countryCode: String,

    @Json(name = "fromAmount")
    val fromAmount: String,

    @Json(name = "toAmount")
    val toAmount: String,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "minFromAmount")
    val minFromAmount: String?,

    @Json(name = "maxFromAmount")
    val maxFromAmount: String?,

    @Json(name = "minToAmount")
    val minToAmount: String?,

    @Json(name = "maxToAmount")
    val maxToAmount: String?,
)