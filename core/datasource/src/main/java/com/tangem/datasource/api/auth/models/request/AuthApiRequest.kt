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
) {

    /** Device metadata collection. */
    @JsonClass(generateAdapter = true)
    data class DeviceMetadata(
        /** Device hardware model (e.g. `iPhone 15 Pro`). */
        @Json(name = "deviceModel") val deviceModel: String?,
        /** Operating system (`android` / `ios`). */
        @Json(name = "os") val os: String,
        /** OS version string (e.g. `17.4.1`). */
        @Json(name = "osVersion") val osVersion: String?,
        /** Application version (e.g. `5.8.0`). */
        @Json(name = "appVersion") val appVersion: String?,
        /** User-Agent header (e.g. `Tangem/5.8.0 (iPhone; iOS 17.4.1; Scale/3.00)`). */
        @Json(name = "userAgent") val userAgent: String?,
        /** Client locale (e.g. `en-US`). */
        @Json(name = "locale") val locale: String?,
        /** Client timezone (e.g. `Europe/Moscow`). */
        @Json(name = "timezone") val timezone: String?,
    )
}