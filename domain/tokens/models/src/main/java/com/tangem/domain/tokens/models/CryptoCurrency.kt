package com.tangem.domain.tokens.models

import java.io.Serializable

/**
 * Represents a generic cryptocurrency.
 *
 * @property id Unique identifier for the cryptocurrency.
 * @property networkId Identifier for the network to which the cryptocurrency belongs.
 * @property name Human-readable name of the cryptocurrency.
 * @property symbol Symbol of the cryptocurrency.
 * @property decimals Number of decimal places used by the cryptocurrency.
 * @property iconUrl Optional URL of the cryptocurrency icon. `null` if not found.
 * @property derivationPath Optional path used for key derivation. `null` if the wallet does not support the
 * [HD Wallet](https://coinsutra.com/hd-wallets-deterministic-wallet/) feature.
 */
sealed class CryptoCurrency : Serializable {

    abstract val id: ID
    abstract val networkId: Network.ID
    abstract val name: String
    abstract val symbol: String
    abstract val decimals: Int
    abstract val iconUrl: String?
    abstract val derivationPath: String?

    /**
     * Represents a native coin in the blockchain network.
     */
    data class Coin(
        override val id: ID,
        override val networkId: Network.ID,
        override val name: String,
        override val symbol: String,
        override val decimals: Int,
        override val iconUrl: String?,
        override val derivationPath: String?,
    ) : CryptoCurrency() {

        init {
            checkProperties()
        }
    }

    /**
     * Represents a token in the blockchain network, typically a non-native asset.
     *
     * @property contractAddress Address of the contract managing the token.
     * @property isCustom Indicates whether the token is a custom user-added token or not.
     */
    data class Token(
        override val id: ID,
        override val networkId: Network.ID,
        override val name: String,
        override val symbol: String,
        override val decimals: Int,
        override val iconUrl: String?,
        override val derivationPath: String?,
        val contractAddress: String,
        val isCustom: Boolean,
        val blockchainName: String, // TODO: Move this field to proper entity
        val standardType: StandardType, // TODO: Move this field to proper entity
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
    data class ID(
        private val prefix: Prefix,
        private val networkId: Network.ID,
        private val suffix: Suffix,
    ) {

        val value: String = buildString {
            append(prefix.value)
            append(networkId.value)
            append(DELIMITER)
            append(suffix.value)
        }

        val rawCurrencyId: String? = (suffix as? Suffix.RawID)?.rawId

        /**
         * Represents the different types of prefixes that can be associated with a cryptocurrency ID.
         * These prefixes can help in quickly categorizing the type of cryptocurrency.
         */
        enum class Prefix(val value: String) {
            /** Prefix for standard coins. */
            COIN_PREFIX(value = "coin_"),

            /** Prefix for standard tokens. */
            TOKEN_PREFIX(value = "token_"),

            /** Prefix for custom tokens. */
            CUSTOM_TOKEN_PREFIX(value = "custom_"),
        }

        /**
         * Represents the suffix part of the cryptocurrency ID.
         *
         * The suffix can either be a raw ID or a contract address.
         */
        sealed class Suffix {

            /** The value of the suffix, which could be either a raw ID or a contract address. */
            abstract val value: String

            /** Represents a raw ID suffix. */
            data class RawID(val rawId: String) : Suffix() {
                override val value: String = rawId
            }

            /** Represents a contract address suffix. */
            data class ContractAddress(val contractAddress: String) : Suffix() {
                override val value: String = contractAddress
            }
        }

        private companion object {
            const val DELIMITER = '#'
        }
    }

    sealed class StandardType {
        abstract val name: String

        object ERC20 : StandardType() {
            override val name: String = "ERC20"
        }
        object TRC20 : StandardType() {
            override val name: String = "TRC20"
        }
        object BEP20 : StandardType() {
            override val name: String = "BEP20"
        }
        object BEP2 : StandardType() {
            override val name: String = "BEP2"
        }
        class Unspecified(val tokenName: String) : StandardType() {
            override val name: String = tokenName
        }
    }

    protected fun checkProperties() {
        require(name.isNotBlank()) { "Crypto currency name must not be blank" }
        require(symbol.isNotBlank()) { "Crypto currency symbol must not be blank" }
        require(iconUrl?.isNotBlank() ?: true) { "Crypto currency icon URL must not be blank" }
        require(decimals > 0) { "Crypto currency decimal must not be less then zero, but it is: $decimals" }
        require(derivationPath?.isNotBlank() ?: true) { "Crypto currency derivation path must not be blank" }
    }
}
