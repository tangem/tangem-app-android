package com.tangem.domain.models.network

import kotlinx.serialization.Serializable

/**
 * Represents a blockchain network, identified by a unique ID, a human-readable name, and its standard type.
 *
 * This class encapsulates the primary details of a blockchain network, such as its ID, name,
 * whether it operates as a test network, and the type of blockchain standard it conforms to
 * (e.g., ERC20, BEP20).
 *
 * @property id                    the unique identifier of the network
 * @property backendId             the name of this network in the Tangem backend
 * @property name                  the human-readable name of the network, such as "Ethereum" or "Bitcoin"
 * @property currencySymbol        the symbol of the currency associated with the network
 * @property derivationPath        the path used to derive keys for this network
 * @property isTestnet             indicates whether the network is a test network or a main network
 * @property standardType          the type of blockchain standard the network adheres to
 * @property hasFiatFeeRate        indicates whether there is a fee in the network that cannot be represented in a fiat
 * currency (for those blockchains that have FeeResource instead of a standard type of fee)
 * @property canHandleTokens       indicates whether the network can handle tokens
 * @property transactionExtrasType the type of extras supported for sending a transaction
 */
@Serializable
data class Network(
    val id: ID,
    val backendId: String,
    val name: String,
    val currencySymbol: String,
    val derivationPath: DerivationPath,
    val isTestnet: Boolean,
    val standardType: StandardType,
    val hasFiatFeeRate: Boolean,
    val canHandleTokens: Boolean,
    val transactionExtrasType: TransactionExtrasType,
) {

    /** Raw ID */
    val rawId: String
        get() = id.rawId.value

    init {
        require(name.isNotBlank()) { "Network name must not be blank" }
        require(id.derivationPath == derivationPath) { "Derivation path must be the same as in the ID" }
    }

    /**
     * Represents a unique identifier for a blockchain network
     *
     * @property rawId          raw network ID
     * @property derivationPath derivation path
     */
    @Serializable
    data class ID(val rawId: RawID, val derivationPath: DerivationPath) {

        init {
            require(rawId.value.isNotBlank()) { "Network ID must not be blank" }
        }

        constructor(value: String, derivationPath: DerivationPath) : this(
            rawId = RawID(value),
            derivationPath = derivationPath,
        )
    }

    @Serializable
    data class RawID(val value: String) {
        override fun toString(): String = value
    }

    /**
     * Represents a path used to derive cryptographic keys for a blockchain network.
     *
     * This class represents such paths in a generic manner, allowing for predefined card-based paths, custom paths,
     * or even no derivation path at all.
     */
    @Serializable
    sealed class DerivationPath {

        /** The actual derivation path value, if any */
        abstract val value: String?

        /** Represents a predefined card-based derivation path [value] */
        @Serializable
        data class Card(override val value: String) : DerivationPath()

        /** Represents a custom derivation path [value] specified by the user */
        @Serializable
        data class Custom(override val value: String) : DerivationPath()

        /** Represents a lack of derivation path, which means the wallet does not support the HD wallet feature */
        @Serializable
        data object None : DerivationPath() {
            override val value: String? get() = null
        }
    }

    /**
     * Represents the type of blockchain standard that a network adheres to.
     *
     * Blockchain networks often follow certain standards that dictate how tokens operate on them.
     * These standards can define functionalities such as how transactions are processed,
     * how tokens are minted or burned, and more.
     */
    @Serializable
    sealed class StandardType {

        /** The human-readable name of the standard type */
        abstract val name: String

        /** Represents the ERC20 token standard, common on the Ethereum network */
        @Serializable
        data object ERC20 : StandardType() {
            override val name: String get() = "ERC20"
        }

        /** Represents the TRC20 token standard, common on the TRON network */
        @Serializable
        data object TRC20 : StandardType() {
            override val name: String get() = "TRC20"
        }

        /** Represents the BEP20 token standard, common on the Binance Smart Chain network */
        @Serializable
        data object BEP20 : StandardType() {
            override val name: String get() = "BEP20"
        }

        /** Represents the BEP2 token standard, common on the Binance Chain network */
        @Serializable
        data object BEP2 : StandardType() {
            override val name: String get() = "BEP2"
        }

        /** Represents a network that does not adhere to a predefined standard type */
        @Serializable
        data class Unspecified(override val name: String) : StandardType()
    }

    /** Represents the supported type of extras for sending a transaction */
    enum class TransactionExtrasType {

        /** No transaction extras supported */
        NONE,

        /** Memo supported */
        MEMO,

        /** Destination tag supported */
        DESTINATION_TAG,

        ;

        /**
         * Indicated whether any tx extras are supported
         */
        fun isTxExtrasSupported() = when (this) {
            NONE -> false
            MEMO,
            DESTINATION_TAG,
            -> true
        }
    }
}