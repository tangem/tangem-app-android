package com.tangem.data.mcp.store

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** An agent the authorization server has described, as it is kept on the device. */
@Serializable
internal data class McpAgentEntity(
    @SerialName("clientId") val clientId: String,
    @SerialName("origin") val origin: String?,
    @SerialName("name") val name: String?,
    @SerialName("iconUrl") val iconUrl: String?,
)