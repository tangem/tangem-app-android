package com.tangem.spend.datasource.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WaitlistRequest(
    @Json(name = "email") val email: String,
    @Json(name = "product") val product: String,
    @Json(name = "country") val country: String,
    @Json(name = "region") val region: String?,
    @Json(name = "marketing_consent") val hasMarketingConsent: Boolean,
)