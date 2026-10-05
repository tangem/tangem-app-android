package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One MCP OAuth authorization issued to an MCP client, as shown to the wallet owner. */
@Serializable
internal data class McpSessionResponse(
    /** Authorization id — pass to `DELETE api/authentication/v1/mcp/sessions/{id}` to revoke. */
    @SerialName("id") val id: String,
    /** OAuth `client_id` of the MCP client. */
    @SerialName("clientId") val clientId: String,
    /**
     * Host the client's metadata document was served from — the only part of a self-registered
     * client's identity the server verified, so prefer it over [clientName] when asking the owner to
     * recognize a client. `null` for clients pre-registered in configuration, which have an opaque
     * `client_id` and no origin.
     */
    @SerialName("clientOrigin") val clientOrigin: String?,
    /**
     * Name the client gave itself in its metadata document. Chosen by the client and not verified —
     * a label, not an identity. `null` for a self-registered client, whose metadata is never persisted.
     */
    @SerialName("clientName") val clientName: String?,
    /** Logo the client gave itself in its metadata document; `null` when it declared none. */
    @SerialName("clientIconUrl") val clientIconUrl: String?,
    /** Scopes granted to the client. */
    @SerialName("scopes") val scopes: List<String>,
    /** IP address the session was established from. */
    @SerialName("clientIp") val clientIp: String?,
    /** Country code resolved from the connecting IP. */
    @SerialName("clientCountry") val clientCountry: String?,
    /** User agent that established the session. */
    @SerialName("clientUserAgent") val clientUserAgent: String?,
)