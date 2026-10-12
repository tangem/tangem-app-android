package com.tangem.spend.datasource.pay.models.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenByCardWalletRequest(
    @SerialName("auth_type") val authType: String = "card_wallet",
    @SerialName("refresh_token") val refreshToken: String,
)