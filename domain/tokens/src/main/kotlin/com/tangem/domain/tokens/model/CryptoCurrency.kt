package com.tangem.domain.tokens.model

sealed class CryptoCurrency {

    abstract val id: ID
    abstract val networkId: Network.ID
    abstract val name: String
    abstract val symbol: String
    abstract val decimals: Int
    abstract val iconUrl: String?
    abstract val derivationPath: String?

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
