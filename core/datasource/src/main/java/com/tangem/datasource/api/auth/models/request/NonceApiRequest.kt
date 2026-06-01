package com.tangem.datasource.api.auth.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Request body for nonce generation (auth, upgrade, wallet flows). */
@JsonClass(generateAdapter = true)
data class NonceApiRequest(
    /** Base64-encoded EC public key of the device (e.g. `MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE...`). */
    @Json(name = "devicePublicKey") val devicePublicKey: String,
)