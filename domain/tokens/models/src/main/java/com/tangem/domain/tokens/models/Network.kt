package com.tangem.domain.tokens.models

import java.io.Serializable

/**
 * Represents a blockchain network, identified by a unique ID, a human-readable name, and its standard type.
 *
 * This class encapsulates the primary details of a blockchain network, such as its ID, name,
 * whether it operates as a test network, and the type of blockchain standard it conforms to
 * (e.g., ERC20, BEP20).
 *
 * @property id The unique identifier of the network.
 * @property name The human-readable name of the network, such as "Ethereum" or "Bitcoin".
 * @property isTestnet Indicates whether the network is a test network or a main network.
 * @property standardType The type of blockchain standard the network adheres to.
 */
// FIXME: Remove serialization [REDACTED_JIRA]
data class Network(
    val id: ID,
    val name: String,
    val isTestnet: Boolean,
    val standardType: StandardType,
) : Serializable {

    init {
        require(name.isNotBlank()) { "Network name must not be blank" }
    }

    /**
     * Represents a unique identifier for a blockchain network.
     *
     * @property value The string representation of the network ID.
     */
    @JvmInline
    value class ID(val value: String) {

        init {
            require(value.isNotBlank()) { "Network ID must not be blank" }
        }
    }

    /**
     * Represents the type of blockchain standard that a network adheres to.
     *
     * Blockchain networks often follow certain standards that dictate how tokens operate on them.
     * These standards can define functionalities such as how transactions are processed,
     * how tokens are minted or burned, and more.
     *
     * @property name The human-readable name of the standard type.
     */
    // FIXME: Remove serialization [REDACTED_JIRA]
    sealed class StandardType : Serializable {
        abstract val name: String

        /** Represents the ERC20 token standard, common on the Ethereum network. */
        object ERC20 : StandardType() {
            override val name: String = "ERC20"
        }

        /** Represents the TRC20 token standard, common on the TRON network. */
        object TRC20 : StandardType() {
            override val name: String = "TRC20"
        }

        /** Represents the BEP20 token standard, common on the Binance Smart Chain network. */
        object BEP20 : StandardType() {
            override val name: String = "BEP20"
        }

        /** Represents the BEP2 token standard, common on the Binance Chain network. */
        object BEP2 : StandardType() {
            override val name: String = "BEP2"
        }

        /** Represents a network that does not adhere to a predefined standard type. */
        class Unspecified(val networkName: String) : StandardType() {
            override val name: String = networkName
        }
    }
}