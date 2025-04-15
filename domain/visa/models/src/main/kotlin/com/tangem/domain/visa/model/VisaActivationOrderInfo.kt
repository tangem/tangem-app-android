package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaActivationOrderInfo(
    @Json(name = "orderId") val orderId: String,
    @Json(name = "customerId") val customerId: String,
    @Json(name = "customerWalletAddress") val customerWalletAddress: String,
)