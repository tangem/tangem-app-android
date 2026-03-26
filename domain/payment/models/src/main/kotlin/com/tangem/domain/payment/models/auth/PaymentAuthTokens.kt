package com.tangem.domain.payment.models.auth

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class PaymentAuthTokens(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "expires_at") val expiresAt: Long,
    @Json(name = "refresh_token") val refreshToken: String,
    @Json(name = "refresh_expires_at") val refreshExpiresAt: Long,
    @Json(name = "idempotency_key") val idempotencyKey: String? = null,
)

fun PaymentAuthTokens.getAuthHeader(): String {
    return "Bearer $accessToken"
}