package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Authentication request — authenticates a previously registered device. */
@JsonClass(generateAdapter = true)
data class AuthApiRequest(
    /** Signed authentication payload. */
    @Json(name = "payload") val payload: AuthenticationPayload,
    /** EC signature over the authentication payload, signed by the device private key (Base64). */
    @Json(name = "signature") val signature: String,
)

/** Signed authentication payload — the data that is signed by the device private key. */
@JsonClass(generateAdapter = true)
data class AuthenticationPayload(
    /** Base64-encoded EC public key of the device (e.g. `MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...`). */
    @Json(name = "devicePublicKey") val devicePublicKey: String,
    /** Deciphered nonce value from the nonce endpoint. */
    @Json(name = "nonce") val nonce: String,
    /** Platform attestation token (Play Integrity / App Attest). */
    @Json(name = "attestationToken") val attestationToken: String?,
    /** Client-reported device metadata. */
    @Json(name = "metadata") val metadata: DeviceMetadata,
)