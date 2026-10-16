package com.tangem.data.mcp.api.models.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Metadata of one MCP client, as the authorization server knows it. */
@Serializable
internal data class McpClientResponse(
    /** OAuth `client_id` of the MCP client, in the form it was asked for. */
    @SerialName("clientId") val clientId: String,
    /** Host the client's metadata document came from; `null` for a client registered in configuration. */
    @SerialName("clientOrigin") val clientOrigin: String?,
    /** Name the client gave itself; `null` when it gave none. */
    @SerialName("clientName") val clientName: String?,
    /** Icon mapped to the verified origin of the client; `null` when none is configured. */
    @SerialName("clientIconUrl") val clientIconUrl: String?,
)