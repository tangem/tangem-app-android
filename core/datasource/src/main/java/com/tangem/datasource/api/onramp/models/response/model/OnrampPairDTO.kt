package com.tangem.datasource.api.onramp.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.onramp.models.common.OnrampDestinationDTO

@JsonClass(generateAdapter = true)
data class OnrampPairDTO(
    @Json(name = "fromCurrencyCode")
    val fromCurrencyCode: String?,

    @Json(name = "to")
    val to: OnrampDestinationDTO,

    @Json(name = "providers")
    val providers: List<OnrampProviderDTO>,
)