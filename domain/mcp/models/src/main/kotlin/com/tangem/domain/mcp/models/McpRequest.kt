package com.tangem.domain.mcp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Plain `type` would clash with the polymorphic class discriminator of the JSON that saves the state
private const val INTENT_TYPE_NAME = "intentType"

/** Something an MCP agent asks the wallet to do, as picked up from one of the entry points. */
@Serializable
sealed interface McpRequest {

    /** A request to connect the agent behind [userCode]. */
    @Serializable
    data class Pair(val userCode: String) : McpRequest

    /** A request to act on the intent [id] raised by [source]. */
    @Serializable
    data class Intent(val id: String, val source: Source) : McpRequest {

        /** The service that raised the intent, together with the operation it asks for. */
        @Serializable
        sealed interface Source {

            @Serializable
            data class Express(@SerialName(INTENT_TYPE_NAME) val type: Type) : Source {

                enum class Type { EXCHANGE }
            }

            @Serializable
            data class Transaction(@SerialName(INTENT_TYPE_NAME) val type: Type) : Source {

                enum class Type { SEND }
            }
        }
    }
}