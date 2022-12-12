package com.tangem.feature.swap.domain.models.data

sealed class Currency(
    open val id: String,
    open val name: String,
    open val symbol: String,
    open val networkId: String,
    open val logoUrl: String,
) {

    data class NativeToken(
        override val id: String,
        override val name: String,
        override val symbol: String,
        override val networkId: String,
        override val logoUrl: String,
    ) : Currency(id, name, symbol, networkId, logoUrl)

    data class NonNativeToken(
        override val id: String,
        override val name: String,
        override val symbol: String,
        override val networkId: String,
        override val logoUrl: String,
        val contractAddress: String,
        val decimalCount: Int,
    ) : Currency(id, name, symbol, networkId, logoUrl)
}
