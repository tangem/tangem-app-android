package com.tangem.domain.tokens.model

data class Network(
    val id: ID,
    val name: String,
) {

    @JvmInline
    value class ID(val value: String)
}
