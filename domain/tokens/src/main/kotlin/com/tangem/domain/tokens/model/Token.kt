package com.tangem.domain.tokens.model

data class Token(
    val id: ID,
    val networkId: Network.ID,
    val name: String,
    val symbol: String,
    val iconUrl: String?,
    val decimals: Int,
    val isCustom: Boolean,
    val isCoin: Boolean,
) {

    @JvmInline
    value class ID(val value: String)
}