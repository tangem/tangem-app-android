package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PromocodeActivationBody(
    @Json(name = "promoCode") val promoCode: String,
    @Json(name = "address") val address: String,
)