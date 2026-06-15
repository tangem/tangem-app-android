package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Registration request — registers a new device and establishes initial trust.
 *
 * Posted to `POST /api/v1/auth/register`; on success the server returns
 * [com.tangem.datasource.api.auth.models.response.TokenApiResponse] (the initial session token pair).
 */
@JsonClass(generateAdapter = true)
data class RegisterApiRequest(
    /** Signed registration payload. */
    @Json(name = "payload") val payload: RegisterPayload,
    /** EC signature over the registration payload, signed by the device private key (Base64). */
    @Json(name = "signature") val signature: String,
)

/** Signed registration payload — the data that is signed by the device private key. */
@JsonClass(generateAdapter = true)
data class RegisterPayload(
    /** Base64-encoded EC public key of the device. */
    @Json(name = "devicePublicKey") val devicePublicKey: String,
    /** Deciphered nonce value from the `/api/v1/auth/nonce/device` endpoint. */
    @Json(name = "nonce") val nonce: String,
    /** Platform attestation token (Play Integrity / App Attest). Optional; backend accepts `null`. */
    @Json(name = "attestationToken") val attestationToken: String?,
    /** Client-reported device metadata. */
    @Json(name = "metadata") val metadata: DeviceMetadata,
)