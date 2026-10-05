package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who is asking for access behind a user code, plus everything the consent call has to echo back.
 *
 * @property clientOrigin host the client's metadata document was served from — the only part of a
 * self-registered client's identity the server verified, so prefer it over [clientName].
 * @property clientName name the client gave itself; not verified on its own, see [isClientNameVerified].
 * @property state one-time value tying the consent to this verification, passed back as a query parameter.
 */
@Serializable
internal data class McpDeviceVerificationResponse(
    @SerialName("client_id") val clientId: String,
    @SerialName("client_origin") val clientOrigin: String?,
    @SerialName("client_name") val clientName: String?,
    @SerialName("client_name_verified") val isClientNameVerified: Boolean?,
    @SerialName("scope") val scope: String,
    @SerialName("state") val state: String,
    @SerialName("user_code") val userCode: String,
)