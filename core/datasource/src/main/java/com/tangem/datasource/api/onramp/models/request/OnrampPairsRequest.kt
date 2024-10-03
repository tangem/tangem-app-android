package com.tangem.datasource.api.onramp.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.onramp.models.common.OnrampDestinationDTO

@JsonClass(generateAdapter = true)
data class OnrampPairsRequest(
    @Json(name = "fromCurrencyCode")
    val fromCurrencyCode: String?,

    @Json(name = "countryCode")
    val countryCode: String,

    @Json(name = "to")
    val to: List<OnrampDestinationDTO>,
)