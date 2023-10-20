package com.tangem.domain.tokens.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

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
@Parcelize
sealed class CryptoCurrency : Parcelable {

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
    @Parcelize
    data class ID(
        private val prefix: Prefix,
        private val body: Body,
        private val suffix: Suffix,
    ) : Parcelable {

        val value: String
            get() = buildString {
                append(prefix.value)
                append(PREFIX_DELIMITER)
                append(body.value)
                append(SUFFIX_DELIMITER)
                append(suffix.value)
            }

        /** Represents a raw cryptocurrency ID. If it is a custom token, the value will be `null`. */
        val rawCurrencyId: String? get() = (suffix as? Suffix.RawID)?.rawId

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
        @Parcelize
        sealed class Body : Parcelable {

            /** The value of the body. */
            abstract val value: String

            /** Represents a raw network ID. */
            data class NetworkId(val rawId: String) : Body() {
                override val value: String get() = rawId
            }

            /**
             * Represents a raw network ID with a network derivation path.
             *
             * Should be used for a cryptocurrencies with custom derivation path.
             * */
            data class NetworkIdWithDerivationPath(
                val rawId: String,
                val derivationPath: String,
            ) : Body() {
                override val value: String
                    get() = buildString {
                        append(rawId)
                        append(DERIVATION_PATH_DELIMITER)
                        append(derivationPath.hashCode())
                    }
            }
        }

        /**
         * Represents the suffix part of the cryptocurrency ID.
         *
         * The suffix can either be a raw ID or a contract address.
         */
        @Parcelize
        sealed class Suffix : Parcelable {

            /** The value of the suffix, which could be either a raw ID or a contract address. */
            abstract val value: String

            /** Represents a raw ID suffix. */
            data class RawID(val rawId: String) : Suffix() {
                override val value: String get() = rawId
            }

            /** Represents a contract address suffix. */
            data class ContractAddress(val contractAddress: String) : Suffix() {
                override val value: String get() = contractAddress
            }
        }

        override fun toString(): String {
            return "ID(value='$value')"
        }

        private companion object {
            const val PREFIX_DELIMITER = '_'
            const val SUFFIX_DELIMITER = '#'
            const val DERIVATION_PATH_DELIMITER = 'd'
        }
    }

    protected fun checkProperties() {
        require(name.isNotBlank()) { "Crypto currency name must not be blank" }
        require(symbol.isNotBlank()) { "Crypto currency symbol must not be blank" }
        require(iconUrl?.isNotBlank() ?: true) { "Crypto currency icon URL must not be blank" }
        require(decimals >= 0) { "Crypto currency decimal must not be less then zero, but it is: $decimals" }
    }
}
