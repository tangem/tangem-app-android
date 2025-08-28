package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GenerateNonceByCustomerWalletRequest(
    @Json(name = "auth_type") val authType: String = "customer_wallet",
    @Json(name = "customer_wallet_address") val customerWalletAddress: String,
)