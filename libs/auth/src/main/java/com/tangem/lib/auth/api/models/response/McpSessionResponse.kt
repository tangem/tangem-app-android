package com.tangem.lib.auth.api.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** One MCP OAuth authorization issued to an MCP client, as shown to the wallet owner. */
@JsonClass(generateAdapter = true)
data class McpSessionResponse(
    /** Authorization id — pass to `DELETE api/authentication/v1/mcp/sessions/{id}` to revoke. */
    @Json(name = "id") val id: String,
    /** OAuth `client_id` of the MCP client. */
    @Json(name = "clientId") val clientId: String,
    /**
     * Host the client's metadata document was served from — the only part of a self-registered
     * client's identity the server verified, so prefer it over [clientName] when asking the owner to
     * recognize a client. `null` for clients pre-registered in configuration, which have an opaque
     * `client_id` and no origin.
     */
    @Json(name = "clientOrigin") val clientOrigin: String?,
    /**
     * Name the client gave itself in its metadata document. Chosen by the client and not verified —
     * a label, not an identity. `null` for a self-registered client, whose metadata is never persisted.
     */
    @Json(name = "clientName") val clientName: String?,
    /** Scopes granted to the client. */
    @Json(name = "scopes") val scopes: List<String>,
    /** IP address the session was established from. */
    @Json(name = "clientIp") val clientIp: String?,
    /** Country code resolved from the connecting IP. */
    @Json(name = "clientCountry") val clientCountry: String?,
    /** User agent that established the session. */
    @Json(name = "clientUserAgent") val clientUserAgent: String?,
)