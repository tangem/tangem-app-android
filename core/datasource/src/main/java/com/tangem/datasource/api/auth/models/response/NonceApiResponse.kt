package com.tangem.datasource.api.auth.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Ciphered nonce response. */
@JsonClass(generateAdapter = true)
data class NonceApiResponse(
    /** RSA-OAEP ciphered nonce value (Base64). */
    @Json(name = "cipheredNonce") val cipheredNonce: String,
    /** Nonce expiration timestamp (ISO-8601). */
    @Json(name = "expiresAt") val expiresAt: String,
)