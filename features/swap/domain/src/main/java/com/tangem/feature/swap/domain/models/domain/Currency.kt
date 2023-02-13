package com.tangem.feature.swap.domain.models.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class Currency {
    abstract val id: String
    abstract val name: String
    abstract val symbol: String
    abstract val networkId: String
    abstract val logoUrl: String

    @Serializable
    data class NativeToken(
        override val id: String,
        override val name: String,
        override val symbol: String,
        override val networkId: String,
        override val logoUrl: String,
    ) : Currency()

    @Serializable
    data class NonNativeToken(
        override val id: String,
        override val name: String,
        override val symbol: String,
        override val networkId: String,
        override val logoUrl: String,
        val contractAddress: String,
        val decimalCount: Int,
    ) : Currency()
}

fun Currency.isNonNative(): Boolean {
    return this is Currency.NonNativeToken
}
