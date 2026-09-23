package com.tangem.lib.auth.api.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Who is asking for access behind a user code, plus everything the consent call has to echo back.
 *
 * @property clientOrigin host the client's metadata document was served from — the only part of a
 * self-registered client's identity the server verified, so prefer it over [clientName].
 * @property clientName name the client gave itself; not verified on its own, see [isClientNameVerified].
 * @property state one-time value tying the consent to this verification, passed back as a query parameter.
 */
@JsonClass(generateAdapter = true)
data class McpDeviceVerificationResponse(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "client_origin") val clientOrigin: String?,
    @Json(name = "client_name") val clientName: String?,
    @Json(name = "client_name_verified") val isClientNameVerified: Boolean?,
    @Json(name = "scope") val scope: String,
    @Json(name = "state") val state: String,
    @Json(name = "user_code") val userCode: String,
)