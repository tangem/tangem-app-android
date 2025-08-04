package com.tangem.domain.models.network

import kotlinx.serialization.Serializable

/** Represents a network address */
@Serializable
sealed class NetworkAddress {

    /** The default or currently selected network address */
    abstract val defaultAddress: Address

    /** The set of available network addresses to choose from */
    abstract val availableAddresses: Set<Address>

    /**
     * Represents a single static network address
     *
     * @property defaultAddress the static network address
     */
    @Serializable
    data class Single(override val defaultAddress: Address) : NetworkAddress() {

        override val availableAddresses: Set<Address> = setOf(defaultAddress)
    }

    /**
     * Represents a network configuration where an address can be chosen from a set of available addresses
     *
     * @property defaultAddress     the currently selected or default network address
     * @property availableAddresses the set of available network addresses to choose from
     */
    @Serializable
    data class Selectable(
        override val defaultAddress: Address,
        override val availableAddresses: Set<Address>,
    ) : NetworkAddress() {

        init {
            require(availableAddresses.isNotEmpty()) { "Available network addresses must not be empty" }
        }
    }

    /**
     * Address
     *
     * @property value string representation of the address
     * @property type  address type
     */
    @Serializable
    data class Address(
        val value: String,
        val type: Type,
    ) {

        enum class Type {
            Primary, Secondary,
        }
    }
}