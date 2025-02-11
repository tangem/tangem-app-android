package com.tangem.domain.visa.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class VisaCardWalletDataToSignRequest(
    @Json(name = "orderId") val orderId: String,
    @Json(name = "customer_id") val customerId: String,
    @Json(name = "customer_wallet_address") val customerWalletAddress: String,
)