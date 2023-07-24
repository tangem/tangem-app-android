package com.tangem.domain.tokens.model

data class Token(
    val id: ID,
    val networkId: Network.ID,
    val name: String,
    val symbol: String,
    val iconUrl: String?,
    val decimals: Int,
    val isCustom: Boolean,
    val contractAddress: String?,
    val derivationPath: String?,
) {

    init {
        require(name.isNotBlank()) { "Token name must not be blank" }
        require(symbol.isNotBlank()) { "Token symbol must not be blank" }
        require(iconUrl?.isNotBlank() ?: true) { "Token icon URL must not be blank" }
        require(decimals > 0) { "Token decimal must not be less then zero, but it is: $decimals" }
        require(contractAddress?.isNotBlank() ?: true) { "Token contract address must not be blank" }
        require(derivationPath?.isNotBlank() ?: true) { "Token derivation path must not be blank" }
    }

    @JvmInline
    value class ID(val value: String) {

        init {
            require(value.isNotBlank()) { "Token ID value must not be blank" }
        }
    }
}