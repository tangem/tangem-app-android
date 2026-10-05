package com.tangem.domain.mcp.models

import kotlinx.serialization.Serializable

/** Something an MCP agent asks the wallet to do, as picked up from one of the entry points. */
@Serializable
sealed interface McpRequest {

    /** A request to connect the agent behind [userCode]. */
    @Serializable
    data class Pair(val userCode: String) : McpRequest
}