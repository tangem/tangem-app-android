package com.tangem.domain.tokens.model

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
sealed class CryptoCurrency {

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
    ) : CryptoCurrency() {

        init {
            checkProperties()
            require(contractAddress.isNotBlank()) { "Token contract address must not be blank" }
        }
    }

    /**
     * Value class for uniquely identifying a cryptocurrency.
     *
     * @property value The unique identifier value.
     */
    @JvmInline
    value class ID(val value: String) {

        init {
            require(value.isNotBlank()) { "Crypto currency ID must not be blank" }
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
