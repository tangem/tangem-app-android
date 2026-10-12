package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Who is asking for access behind a user code, plus everything the consent call has to echo back.
 *
 * @property clientOrigin host the client's metadata document was served from — the only part of a
 * self-registered client's identity the server verified, so prefer it over [clientName].
 * @property clientName name the client gave itself; chosen by the client and not verified.
 * @property state one-time value tying the consent to this verification, passed back as a query parameter.
 */
@Serializable
internal data class McpDeviceVerificationResponse(
    @SerialName("clientId") val clientId: String,
    @SerialName("clientOrigin") val clientOrigin: String?,
    @SerialName("clientName") val clientName: String?,
    @SerialName("clientIconUrl") val clientIconUrl: String?,
    @SerialName("scope") val scope: String,
    @SerialName("state") val state: String,
    @SerialName("userCode") val userCode: String,
)