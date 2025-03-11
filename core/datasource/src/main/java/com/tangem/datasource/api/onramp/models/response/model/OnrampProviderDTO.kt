package com.tangem.datasource.api.onramp.models.response.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampProviderDTO(
    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "paymentMethods")
    val paymentMethods: List<String>,
)