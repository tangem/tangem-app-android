package com.tangem.domain.mcp.models

import kotlinx.serialization.Serializable

/** Something an MCP agent asks the wallet to do, as picked up from one of the entry points. */
@Serializable
sealed interface McpRequest {

    /** A request to connect the agent behind [userCode]. */
    @Serializable
    data class Pair(val userCode: String) : McpRequest

    /** A request to act on the intent [id] raised by [source], of the given [type]. */
    @Serializable
    data class Intent(val id: String, val source: String, val type: String) : McpRequest {

        companion object {
            /** The only service whose intents the app can act on so far. */
            const val SOURCE_EXPRESS = "express"

            /** The only operation of [SOURCE_EXPRESS] intents the app can act on so far. */
            const val TYPE_EXCHANGE = "exchange"
        }
    }
}