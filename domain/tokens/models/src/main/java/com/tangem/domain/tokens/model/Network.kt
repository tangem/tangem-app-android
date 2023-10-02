package com.tangem.domain.tokens.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a blockchain network, identified by a unique ID, a human-readable name, and its standard type.
 *
 * This class encapsulates the primary details of a blockchain network, such as its ID, name,
 * whether it operates as a test network, and the type of blockchain standard it conforms to
 * (e.g., ERC20, BEP20).
 *
 * @property id The unique identifier of the network.
 * @property name The human-readable name of the network, such as "Ethereum" or "Bitcoin".
 * @property derivationPath The path used to derive keys for this network.
 * @property isTestnet Indicates whether the network is a test network or a main network.
 * @property standardType The type of blockchain standard the network adheres to.
 */
@Parcelize
data class Network(
    val id: ID,
    val name: String,
    val derivationPath: DerivationPath,
    val isTestnet: Boolean,
    val standardType: StandardType,
) : Parcelable {

    init {
        require(name.isNotBlank()) { "Network name must not be blank" }
    }

    /**
     * Represents a unique identifier for a blockchain network.
     *
     * @property value The string representation of the network ID.
     */
    @JvmInline
    @Parcelize
    value class ID(val value: String) : Parcelable {

        init {
            require(value.isNotBlank()) { "Network ID must not be blank" }
        }
    }

    /**
     * Represents a path used to derive cryptographic keys for a blockchain network.
     *
     * This class represents such paths in a generic manner, allowing for predefined card-based paths,
     * custom paths, or even no derivation path at all.
     */
    @Parcelize
    sealed class DerivationPath : Parcelable {

        /** The actual derivation path value, if any. */
        abstract val value: String?

        /**
         * Represents a predefined card-based derivation path.
         *
         * @property value The derivation path string.
         */
        data class Card(override val value: String) : DerivationPath()

        /**
         * Represents a custom derivation path specified by the user.
         *
         * @property value The derivation path string.
         */
        data class Custom(override val value: String) : DerivationPath()

        /**
         * Represents a lack of derivation path.
         */
        object None : DerivationPath() {
            override val value: String? get() = null
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
    @Parcelize
    sealed class StandardType : Parcelable {
        abstract val name: String

        /** Represents the ERC20 token standard, common on the Ethereum network. */
        object ERC20 : StandardType() {
            override val name: String get() = "ERC20"
        }

        /** Represents the TRC20 token standard, common on the TRON network. */
        object TRC20 : StandardType() {
            override val name: String get() = "TRC20"
        }

        /** Represents the BEP20 token standard, common on the Binance Smart Chain network. */
        object BEP20 : StandardType() {
            override val name: String get() = "BEP20"
        }

        /** Represents the BEP2 token standard, common on the Binance Chain network. */
        object BEP2 : StandardType() {
            override val name: String get() = "BEP2"
        }

        /** Represents a network that does not adhere to a predefined standard type. */
        data class Unspecified(override val name: String) : StandardType()
    }
}
