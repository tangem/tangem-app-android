package com.tangem.domain.tokens.models

/**
 * Represents a blockchain network, identified by a unique ID and a human-readable name.
 *
 * @property id The unique identifier of the network, encapsulated as an inline value class.
 * @property name The human-readable name of the network, such as "Ethereum" or "Bitcoin".
 *
 * @throws IllegalArgumentException If the name or ID is blank.
 */
data class Network(val id: ID, val name: String) {

    init {
        require(name.isNotBlank()) { "Network name must not be blank" }
    }

    /**
     * Represents a unique identifier for a network.
     *
     * @property value The string value of the network ID.
     */
    @JvmInline
    value class ID(val value: String) {

        init {
            require(value.isNotBlank()) { "Network ID must not be blank" }
        }
    }
}
