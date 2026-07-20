package com.tangem.domain.polymarket.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Polymarket CLOB L2 API credentials, derived once during onboarding from an L1 signature.
 *
 * Used to sign CLOB requests with an HMAC header; must be persisted so that a re-derivation
 * (and therefore another card tap) is not required on every app launch.
 *
 * @property apiKey     public identifier of the derived API key
 * @property secret     base64 HMAC secret used to sign CLOB requests
 * @property passphrase passphrase sent alongside the HMAC signature
 */
@JsonClass(generateAdapter = true)
data class PolymarketApiCredentials(
    @Json(name = "apiKey") val apiKey: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "passphrase") val passphrase: String,
)