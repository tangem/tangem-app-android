package com.tangem.domain.visa.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TangemPayAuthTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_at") val expiresAt: Long,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("refresh_expires_at") val refreshExpiresAt: Long,
    @SerialName("idempotency_key") val idempotencyKey: String? = null,
)

fun TangemPayAuthTokens.getAuthHeader(): String {
    return "Bearer $accessToken"
}