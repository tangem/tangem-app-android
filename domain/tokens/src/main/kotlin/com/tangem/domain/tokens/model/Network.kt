package com.tangem.domain.tokens.model

data class Network(
    val id: ID,
    val name: String,
) {

    init {
        require(name.isNotBlank()) { "Network name must not be blank" }
    }

    @JvmInline
    value class ID(val value: String) {

        init {
            require(value.isNotBlank()) { "Network ID must not be blank" }
        }
    }
}
