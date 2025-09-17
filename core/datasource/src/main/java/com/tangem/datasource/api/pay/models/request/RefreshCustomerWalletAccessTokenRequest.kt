package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RefreshCustomerWalletAccessTokenRequest(
    @Json(name = "auth_type") val authType: String = "customer_wallet",
    @Json(name = "refresh_token") val refreshToken: String,
)