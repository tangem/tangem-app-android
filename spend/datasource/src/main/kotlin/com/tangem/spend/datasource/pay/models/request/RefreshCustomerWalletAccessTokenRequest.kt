package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshCustomerWalletAccessTokenRequest(
    @SerialName("auth_type") val authType: String,
    @SerialName("refresh_token") val refreshToken: String,
)