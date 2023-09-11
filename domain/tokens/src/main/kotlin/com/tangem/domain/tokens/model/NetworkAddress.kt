package com.tangem.domain.tokens.model

/**
 * Represents a network address configuration.
 */
sealed class NetworkAddress {

    /** The default or currently selected network address. */
    abstract val defaultAddress: String

    /**
     * Represents a single static network address.
     *
     * @property defaultAddress The static network address.
     */
    data class Single(override val defaultAddress: String) : NetworkAddress() {

        init {
            checkDefaultAddress()
        }
    }

    /**
     * Represents a network configuration where an address can be chosen from a set of available addresses.
     *
     * @property defaultAddress The currently selected or default network address.
     * @property availableAddresses The set of available network addresses to choose from.
     */
    data class Selectable(
        override val defaultAddress: String,
        val availableAddresses: Set<String>,
    ) : NetworkAddress() {

        init {
            checkDefaultAddress()
            require(availableAddresses.isNotEmpty()) { "Available network addresses must not be empty" }
        }
    }

    protected fun checkDefaultAddress() {
        require(defaultAddress.isNotBlank()) { "Selected network address must not be blank" }
    }
}