package com.tangem.domain.tokens.model

import kotlinx.serialization.Serializable

/**
 * Represents a generic cryptocurrency.
 *
 * @property id Unique identifier for the cryptocurrency.
 * @property network The network to which the cryptocurrency belongs.
 * @property name Human-readable name of the cryptocurrency.
 * @property symbol Symbol of the cryptocurrency.
 * @property decimals Number of decimal places used by the cryptocurrency.
 * @property iconUrl Optional URL of the cryptocurrency icon. `null` if not found.
 * @property isCustom Indicates whether the currency is a custom user-added currency or not.
 */
@Serializable
sealed class CryptoCurrency {

    abstract val id: ID
    abstract val network: Network
    abstract val name: String
    abstract val symbol: String
    abstract val decimals: Int
    abstract val iconUrl: String?
    abstract val isCustom: Boolean

    /**
     * Represents a native coin in the blockchain network.
     */
    @Serializable
    data class Coin(
        override val id: ID,
        override val network: Network,
        override val name: String,
        override val symbol: String,
        override val decimals: Int,
        override val iconUrl: String?,
        override val isCustom: Boolean,
    ) : CryptoCurrency() {

        init {
            checkProperties()
        }
    }

    /**
     * Represents a token in the blockchain network, typically a non-native asset.
     *
     * @property contractAddress Address of the contract managing the token.
     */
    @Serializable
    data class Token(
        override val id: ID,
        override val network: Network,
        override val name: String,
        override val symbol: String,
        override val decimals: Int,
        override val iconUrl: String?,
        override val isCustom: Boolean,
        val contractAddress: String,
    ) : CryptoCurrency() {

        init {
            checkProperties()
            require(contractAddress.isNotBlank()) { "Token contract address must not be blank" }
        }
    }

    /**
     * Represents a unique identifier for a cryptocurrency, constructed from various components.
     *
     * The ID is designed to ensure that different cryptocurrencies, whether they are standard tokens, custom tokens or
     * standard coins, can be distinctly identified within a system.
     *
     * @property value Constructed unique identifier value, made up of prefix, network ID, and suffix.
     * @property rawCurrencyId Represents not unique currency ID from the blockchain network. `null` if
     * its ID of the custom token.
     */
    @Serializable
    data class ID(
        private val prefix: Prefix,
        private val body: Body,
        private val suffix: Suffix,
    ) {

        val value: String
            get() = buildString {
                append(prefix.value)
                append(PREFIX_DELIMITER)
                append(body.value)
                append(SUFFIX_DELIMITER)
                append(suffix.value)
            }

        /** Represents a raw cryptocurrency ID. If it is a custom token, the value will be `null`. */
        val rawCurrencyId: RawID? get() = (suffix as? Suffix.RawID)?.rawId?.let { RawID(it) }

        val contractAddress: String? get() = (suffix as? Suffix.RawID)?.contractAddress

        /** Represents a raw cryptocurrency's network ID. */
        val rawNetworkId: String
            get() = when (body) {
                is Body.NetworkId -> body.rawId
                is Body.NetworkIdWithDerivationPath -> body.rawId
            }

        /**
         * Represents the different types of prefixes that can be associated with a cryptocurrency ID.
         *
         * These prefixes can help in quickly categorizing the type of cryptocurrency.
         */
        enum class Prefix(val value: String) {
            /** Prefix for standard coins. */
            COIN_PREFIX(value = "coin"),

            /** Prefix for standard tokens. */
            TOKEN_PREFIX(value = "token"),
        }

        /**
         * Represents the body part of the cryptocurrency ID.
         *
         * The body can be either a raw network ID or a raw network ID with a network derivation path.
         */
        @Serializable
        sealed class Body {

            /** The value of the body. */
            abstract val value: String

            @Serializable
            /** Represents a raw network ID. */
            data class NetworkId(val rawId: String) : Body() {
                override val value: String get() = rawId
            }

            /**
             * Represents a raw network ID with a network derivation path.
             *
             * Should be used for a cryptocurrencies with custom derivation path.
             * */
            @Serializable
            data class NetworkIdWithDerivationPath(
                val rawId: String,
                val derivationPathHashCode: Int,
            ) : Body() {

                override val value: String
                    get() = buildString {
                        append(rawId)
                        append(DERIVATION_PATH_DELIMITER)
                        append(derivationPathHashCode)
                    }

                constructor(rawId: String, derivationPath: String) : this(
                    rawId = rawId,
                    derivationPathHashCode = derivationPath.hashCode(),
                )
            }
        }

        /**
         * Represents the suffix part of the cryptocurrency ID.
         *
         * The suffix can either be a raw ID or a contract address.
         */
        @Serializable
        sealed class Suffix {

            /** The value of the suffix, which could be either a raw ID or a contract address. */
            abstract val value: String

            /** Represents a raw ID suffix. */
            @Serializable
            data class RawID(val rawId: String, val contractAddress: String? = null) : Suffix() {
                override val value: String
                    get() = buildString {
                        append(rawId)
                        if (contractAddress != null) {
                            append(CONTRACT_ADDRESS_DELIMITER)
                            append(contractAddress)
                        }
                    }
            }

            /** Represents a contract address suffix. */
            @Serializable
            data class ContractAddress(val contractAddress: String) : Suffix() {
                override val value: String get() = contractAddress
            }
        }

        override fun toString(): String {
            return "ID(value='$value')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ID

            return value == other.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }

        companion object {
            // should use delimiters that could be used in URL not like path or query delimiters
            private const val PREFIX_DELIMITER = '\u27E8' // ⟨
            private const val SUFFIX_DELIMITER = '\u27E9' // ⟩
            private const val CONTRACT_ADDRESS_DELIMITER = '\u2693' // ⚓
            private const val DERIVATION_PATH_DELIMITER = '\u2192' // →

            private const val ID_PARTS_COUNT = 3

            /**
             * Creates an [ID] from a string [value].
             *
             * Example:
             * 1. coin⟨BCH⟩bitcoin-cash
             * 2. coin⟨ETH→12367123⟩ethereum
             */
            fun fromValue(value: String): ID {
                // ID(value='coin⟨BCH⟩bitcoin-cash'
                val parts = value.split(PREFIX_DELIMITER, SUFFIX_DELIMITER)

                require(value = parts.size == ID_PARTS_COUNT) { "Invalid ID format: $value" }

                val prefix = Prefix.entries.firstOrNull { it.value == parts[0] }
                requireNotNull(prefix) { "Invalid ID prefix: ${parts[0]}" }

                val bodyParts = parts[1].split(DERIVATION_PATH_DELIMITER)
                val body = when (bodyParts.size) {
                    1 -> Body.NetworkId(bodyParts[0])
                    2 -> Body.NetworkIdWithDerivationPath(
                        rawId = bodyParts[0],
                        derivationPathHashCode = bodyParts[1].toInt(),
                    )
                    else -> error("Invalid ID body: ${parts[1]}")
                }

                val suffixParts = parts[2].split(CONTRACT_ADDRESS_DELIMITER)
                val suffix = when (suffixParts.size) {
                    1 -> Suffix.ContractAddress(suffixParts[0])
                    2 -> Suffix.RawID(suffixParts[0], suffixParts[1])
                    else -> error("Invalid ID suffix: ${parts[2]}")
                }

                return ID(prefix, body, suffix)
            }
        }
    }

    /**
     * Represents a raw cryptocurrency ID. Used for backend calls.
     * Use with caution, as it does not provide the same level of uniqueness as [ID].
     */
    @Serializable
    @JvmInline
    value class RawID(val value: String) {
        override fun toString(): String = value
    }

    protected fun checkProperties() {
        require(name.isNotBlank()) { "Crypto currency name must not be blank" }
        require(symbol.isNotBlank()) { "Crypto currency symbol must not be blank" }
        require(iconUrl?.isNotBlank() ?: true) { "Crypto currency icon URL must not be blank" }
        require(decimals >= 0) { "Crypto currency decimal must not be less then zero, but it is: $decimals" }
    }
}